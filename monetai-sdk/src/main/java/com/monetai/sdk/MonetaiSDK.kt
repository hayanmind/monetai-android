package com.monetai.sdk

import android.content.Context
import android.util.Log
import com.monetai.sdk.billing.BillingManager
import com.monetai.sdk.billing.ReceiptValidator
import com.monetai.sdk.models.*
import com.monetai.sdk.network.ApiClient
import com.monetai.sdk.network.ApiRequests
import com.monetai.sdk.paywall.MonetaiPaywallManager
import com.monetai.sdk.banner.MonetaiBannerManager
import com.monetai.sdk.utils.DateTimeHelper
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import com.jakewharton.threetenabp.AndroidThreeTen

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
    private var serverTimeOffset: Long = 0L
    private val pendingEvents = ConcurrentLinkedQueue<PendingEvent>()

    // Application context for UI components
    private var applicationContext: Context? = null
    // Billing components
    private var billingManager: BillingManager? = null
    private var receiptValidator: ReceiptValidator? = null

    // Paywall and Banner components
    internal val paywallManager = MonetaiPaywallManager()
    internal val bannerManager: MonetaiBannerManager by lazy {
        MonetaiBannerManager(applicationContext ?: throw IllegalStateException("SDK not initialized. Call initialize() first."))
    }

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

                // Minimal main-thread section: AndroidThreeTen init, Billing setup
                withContext(Dispatchers.Main) {
                    // Initialize ThreeTenABP for timezone support
                    AndroidThreeTen.init(context)

                    // Store SDK key and user ID in memory
                    this@MonetaiSDK.sdkKey = sdkKey
                    this@MonetaiSDK.userId = userId
                    this@MonetaiSDK.applicationContext = context

                    // Start billing observation (BillingClient requires main thread)
                    billingManager = BillingManager(context, sdkKey, userId)
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
                        receiptValidator = ReceiptValidator(context, sdkKey, userId)
                        receiptValidator?.sendReceipt()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send receipt", e)
                    }
                }

                // API initialization (IO)
                val initResponse = ApiRequests.initialize(sdkKey = sdkKey, userId = userId)

                // Calculate server time offset
                val clientTimestamp = System.currentTimeMillis()
                this@MonetaiSDK.serverTimeOffset = initResponse.server_timestamp - clientTimestamp

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
        val sdkKey = sdkKey
        val userId = userId

        if (sdkKey == null || userId == null) {
            pendingEvents.offer(PendingEvent.LogEvent(options, System.currentTimeMillis()))
            return
        }

        internalScope.launch {
            try {
                val adjustedTimestamp = Date(options.createdAt.time + serverTimeOffset)
                val createdAt = DateTimeHelper.formatToISO8601(adjustedTimestamp)
                ApiRequests.createEvent(
                    sdkKey = sdkKey,
                    userId = userId,
                    eventName = options.eventName,
                    params = options.params,
                    createdAt = createdAt
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
     * @param promotionId Promotion ID
     * @param completion Completion callback with offer or error
     */
    fun getOffer(promotionId: Int, completion: ((Offer?, Exception?) -> Unit)? = null) {
        internalScope.launch {
            try {
                val sdkKey = sdkKey ?: throw MonetaiError.NotInitialized
                val userId = userId ?: throw MonetaiError.NotInitialized

                val offer = ApiRequests.getOffer(sdkKey = sdkKey, userId = userId, promotionId = promotionId)

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
        val sdkKey = sdkKey
        val userId = userId

        if (sdkKey == null || userId == null) {
            pendingEvents.offer(PendingEvent.ViewProductItem(params, System.currentTimeMillis()))
            return
        }

        internalScope.launch {
            try {
                val adjustedTimestamp = Date(System.currentTimeMillis() + serverTimeOffset)
                val createdAt = DateTimeHelper.formatToISO8601(adjustedTimestamp)
                ApiRequests.logViewProductItem(
                    sdkKey = sdkKey,
                    userId = userId,
                    params = params,
                    createdAt = createdAt
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
        serverTimeOffset = 0L
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

    /**
     * Get application context
     */
    fun getApplicationContext(): Context? = applicationContext

    // MARK: - Private Methods

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
                        val adjustedTimestamp = Date(event.clientTimestamp + serverTimeOffset)
                        val createdAt = DateTimeHelper.formatToISO8601(adjustedTimestamp)
                        ApiRequests.createEvent(
                            sdkKey = sdkKey,
                            userId = userId,
                            eventName = event.options.eventName,
                            params = event.options.params,
                            createdAt = createdAt
                        )
                    }
                    is PendingEvent.ViewProductItem -> {
                        val adjustedTimestamp = Date(event.clientTimestamp + serverTimeOffset)
                        val createdAt = DateTimeHelper.formatToISO8601(adjustedTimestamp)
                        ApiRequests.logViewProductItem(
                            sdkKey = sdkKey,
                            userId = userId,
                            params = event.params,
                            createdAt = createdAt
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process pending event", e)
            }
        }
    }
}
