package com.monetai.sdk

import android.content.Context
import android.util.Log
import com.monetai.sdk.billing.BillingManager
import com.monetai.sdk.billing.ReceiptValidator
import com.monetai.sdk.models.*
import com.monetai.sdk.network.ApiClient
import com.monetai.sdk.network.ApiRequests
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Main Monetai SDK class
 * Provides dynamic pricing functionality for Android apps
 */
class MonetaiSDK private constructor() {

    companion object {
        private const val TAG = "MonetaiSDK"

        @JvmStatic
        val shared: MonetaiSDK by lazy { MonetaiSDK() }
    }

    // MARK: - Pending Event Types
    sealed class PendingEvent {
        data class LogEvent(val options: LogEventOptions, val clientTimestamp: Long) : PendingEvent()
        data class ViewProductItem(val params: ViewProductItemParams, val clientTimestamp: Long) : PendingEvent()
    }

    // MARK: - Properties
    @Volatile
    private var isInitialized: Boolean = false

    private var sdkKey: String? = null
    private var userId: String? = null
    private var organizationId: Int? = null
    private val pendingEvents = ConcurrentLinkedQueue<PendingEvent>()

    // Microsecond timestamp anchoring
    // Captured once at class init; used to derive μs-precision timestamps via monotonic clock
    private val performanceOriginUs: Long = System.currentTimeMillis() * 1000L
    private val nanoTimeOrigin: Long = System.nanoTime()
    private var serverTimeOffsetUs: Long = 0L

    // Billing components
    private var billingManager: BillingManager? = null
    private var receiptValidator: ReceiptValidator? = null

    // Coroutine scope for internal operations
    private val internalScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // MARK: - Internal Properties
    internal val currentSDKKey: String? get() = sdkKey
    internal val currentUserId: String? get() = userId

    // MARK: - Public Methods

    /**
     * Initialize MonetaiSDK
     * @param context Application context
     * @param sdkKey SDK key (required)
     * @param userId User unique ID (required)
     * @param completion Completion callback with result or error
     */
    fun initialize(
        context: Context,
        sdkKey: String,
        userId: String,
        completion: ((InitializeResult?, Exception?) -> Unit)? = null
    ) {
        internalScope.launch {
            try {
                // Validation
                require(sdkKey.isNotEmpty()) { "SDK key cannot be empty" }
                require(userId.isNotEmpty()) { "User ID cannot be empty" }

                // Reset if already initialized with different credentials
                if (isInitialized && (this@MonetaiSDK.sdkKey != sdkKey || this@MonetaiSDK.userId != userId)) {
                    reset()
                }

                // Minimal main-thread section: Billing setup
                withContext(Dispatchers.Main) {
                    // Store SDK key and user ID in memory
                    this@MonetaiSDK.sdkKey = sdkKey
                    this@MonetaiSDK.userId = userId
                    // Start billing observation (BillingClient requires main thread)
                    billingManager = BillingManager(context, sdkKey, userId, internalScope)
                    billingManager?.startObserving()
                }

                // Update SDK header interceptor with app info
                try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    ApiClient.sdkHeaderInterceptor.appVersion = packageInfo.versionName ?: ""
                    ApiClient.sdkHeaderInterceptor.packageName = context.packageName
                    ApiClient.sdkHeaderInterceptor.userId = userId
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get package info for SDK headers", e)
                }

                // Send receipt asynchronously in background (does not block initialization)
                launch {
                    try {
                        receiptValidator = ReceiptValidator(context, sdkKey, userId, internalScope)
                        receiptValidator?.sendReceipt()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send receipt", e)
                    }
                }

                // API initialization (IO)
                val initResponse = ApiRequests.initialize(sdkKey = sdkKey, userId = userId)

                // Calculate server time offset in microseconds for timestamp field
                val clientTimestampUs = currentTimestampUs()
                this@MonetaiSDK.serverTimeOffsetUs = (initResponse.server_timestamp * 1000L) - clientTimestampUs

                // Store initialization data (IO)
                this@MonetaiSDK.organizationId = initResponse.organization_id

                // Initialization complete (IO)
                isInitialized = true

                // Process pending events (IO)
                processPendingEvents()

                val result = InitializeResult(
                    organizationId = initResponse.organization_id,
                    platform = initResponse.platform,
                    version = initResponse.version,
                    userId = userId
                )

                withContext(Dispatchers.Main) {
                    completion?.invoke(result, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "SDK initialization failed", e)
                withContext(Dispatchers.Main) {
                    completion?.invoke(null, e)
                }
            }
        }
    }

    /**
     * Log event (using LogEventOptions)
     * @param options Event options to log
     */
    fun logEvent(options: LogEventOptions) {
        // 호출 시점에 즉시 타임스탬프 캡처
        val clientTimestampUs = currentTimestampUs()

        val sdkKey = sdkKey
        val userId = userId

        if (sdkKey == null || userId == null) {
            pendingEvents.offer(PendingEvent.LogEvent(options, clientTimestampUs))
            return
        }

        val timestamp = toServerTimestampUs(clientTimestampUs)
        internalScope.launch {
            try {
                ApiRequests.createEvent(
                    sdkKey = sdkKey,
                    userId = userId,
                    eventName = options.eventName,
                    params = options.params,
                    timestamp = timestamp
                )
            } catch (e: Exception) {
                Log.e(TAG, "Event logging failed: ${options.eventName}", e)
            }
        }
    }

    /**
     * Log event (basic method)
     * @param eventName Event name
     * @param params Event parameters (optional)
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        val options = LogEventOptions(eventName = eventName, params = params)
        logEvent(options)
    }

    /**
     * Get dynamic pricing offer for a promotion
     * @param placement Placement identifier for the promotion
     * @param completion Completion callback with offer or error
     */
    fun getOffer(placement: String, completion: ((Offer?, Exception?) -> Unit)? = null) {
        internalScope.launch {
            try {
                val sdkKey = sdkKey ?: throw MonetaiError.NotInitialized
                val userId = userId ?: throw MonetaiError.NotInitialized

                val offer = ApiRequests.getOffer(sdkKey = sdkKey, userId = userId, placement = placement)

                withContext(Dispatchers.Main) {
                    completion?.invoke(offer, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get offer", e)
                withContext(Dispatchers.Main) {
                    completion?.invoke(null, e)
                }
            }
        }
    }

    /**
     * Log view product item event
     * @param params View product item parameters
     */
    fun logViewProductItem(params: ViewProductItemParams) {
        // 호출 시점에 즉시 타임스탬프 캡처
        val clientTimestampUs = currentTimestampUs()

        val sdkKey = sdkKey
        val userId = userId

        if (sdkKey == null || userId == null) {
            pendingEvents.offer(PendingEvent.ViewProductItem(params, clientTimestampUs))
            return
        }

        val timestamp = toServerTimestampUs(clientTimestampUs)
        internalScope.launch {
            try {
                ApiRequests.logViewProductItem(
                    sdkKey = sdkKey,
                    userId = userId,
                    params = params,
                    timestamp = timestamp
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log view product item", e)
            }
        }
    }

    /**
     * Reset SDK
     */
    fun reset() {
        sdkKey = null
        userId = null
        organizationId = null
        serverTimeOffsetUs = 0L
        isInitialized = false
        pendingEvents.clear()

        // Stop billing observation
        billingManager?.stopObserving()
        billingManager = null
        receiptValidator = null

        // Cancel internal coroutines
        internalScope.coroutineContext.cancelChildren()
    }

    /**
     * Return current user ID
     */
    fun getUserId(): String? = userId

    /**
     * Return current SDK key
     */
    fun getSdkKey(): String? = sdkKey

    /**
     * Return SDK initialization status
     */
    fun getInitialized(): Boolean = isInitialized

    // MARK: - Private Methods

    /**
     * Generate a microsecond-precision Unix timestamp.
     * Uses monotonic clock (System.nanoTime()) anchored to performanceOriginUs
     * to avoid wall-clock jumps while maintaining μs precision.
     */
    private fun currentTimestampUs(): Long {
        val elapsedUs = (System.nanoTime() - nanoTimeOrigin) / 1000L
        return performanceOriginUs + elapsedUs
    }

    /**
     * Adjusts a client μs timestamp to server time using the calculated offset.
     */
    private fun toServerTimestampUs(clientTimestampUs: Long): Long {
        return clientTimestampUs + serverTimeOffsetUs
    }

    private suspend fun processPendingEvents() {
        val sdkKey = sdkKey ?: return
        val userId = userId ?: return

        val events = mutableListOf<PendingEvent>()
        while (pendingEvents.isNotEmpty()) {
            pendingEvents.poll()?.let { events.add(it) }
        }

        if (events.isEmpty()) {
            return
        }

        events.forEach { event ->
            try {
                when (event) {
                    is PendingEvent.LogEvent -> {
                        val timestampUs = toServerTimestampUs(event.clientTimestamp)
                        ApiRequests.createEvent(
                            sdkKey = sdkKey,
                            userId = userId,
                            eventName = event.options.eventName,
                            params = event.options.params,
                            timestamp = timestampUs
                        )
                    }
                    is PendingEvent.ViewProductItem -> {
                        val timestampUs = toServerTimestampUs(event.clientTimestamp)
                        ApiRequests.logViewProductItem(
                            sdkKey = sdkKey,
                            userId = userId,
                            params = event.params,
                            timestamp = timestampUs
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process pending event", e)
            }
        }
    }
}
