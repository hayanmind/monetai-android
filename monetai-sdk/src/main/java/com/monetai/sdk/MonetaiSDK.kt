package com.monetai.sdk

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.monetai.sdk.billing.BillingManager
import com.monetai.sdk.billing.ReceiptValidator
import com.monetai.sdk.models.*
import com.monetai.sdk.network.ApiRequests
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import com.jakewharton.threetenabp.AndroidThreeTen

/**
 * Main Monetai SDK class
 * Provides AI-powered purchase prediction functionality for Android apps
 */
class MonetaiSDK private constructor() {
    
    companion object {
        private const val TAG = "MonetaiSDK"
        
        @JvmStatic
        val shared: MonetaiSDK by lazy { MonetaiSDK() }
    }
    
    // MARK: - Properties
    @Volatile
    private var isInitialized: Boolean = false
    private var exposureTimeSec: Int? = null
    private var currentDiscount: AppUserDiscount? = null
    
    private var sdkKey: String? = null
    private var userId: String? = null
    private var campaign: Campaign? = null
    private var organizationId: Int? = null
    private var abTestGroup: ABTestGroup? = null
    private val pendingEvents = ConcurrentLinkedQueue<LogEventOptions>()
    
    // SharedPreferences for storing SDK data
    private var sharedPreferences: SharedPreferences? = null
    
    // Billing components
    private var billingManager: BillingManager? = null
    private var receiptValidator: ReceiptValidator? = null
    
    // Coroutine scope for internal operations
    private val internalScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // MARK: - Event Callbacks
    var onDiscountInfoChange: ((AppUserDiscount?) -> Unit)? = null
        set(value) {
            field = value
            // If callback is set and we already have discount info, trigger it immediately
            if (value != null && currentDiscount != null) {
                value(currentDiscount)
            }
        }
    
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
                val result = withContext(Dispatchers.Main) {
                    // Validation
                    require(sdkKey.isNotEmpty()) { "SDK key cannot be empty" }
                    require(userId.isNotEmpty()) { "User ID cannot be empty" }
                    
                    // Reset if already initialized with different credentials
                    if (isInitialized && (this@MonetaiSDK.sdkKey != sdkKey || this@MonetaiSDK.userId != userId)) {
                        Log.d(TAG, "SDK already initialized with different credentials - resetting")
                        reset()
                    }
                    
                    Log.d(TAG, "Initializing Monetai SDK...")
                    
                    // Initialize ThreeTenABP for timezone support
                    AndroidThreeTen.init(context)
                    
                    // Initialize SharedPreferences
                    sharedPreferences = context.getSharedPreferences("MonetaiPrefs", Context.MODE_PRIVATE)
                    
                    // Store SDK key and user ID in SharedPreferences
                    sharedPreferences?.edit()
                        ?.putString("MonetaiSdkKey", sdkKey)
                        ?.putString("MonetaiAppAccountToken", userId)
                        ?.apply()
                    
                    // Store SDK key and user ID in memory
                    this@MonetaiSDK.sdkKey = sdkKey
                    this@MonetaiSDK.userId = userId
                    
                    // Start billing observation
                    billingManager = BillingManager(context)
                    billingManager?.startObserving()
                    
                    // Send receipt
                    launch {
                        try {
                            receiptValidator = ReceiptValidator(context)
                            receiptValidator?.sendReceipt()
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to send receipt", e)
                        }
                    }
                    
                    // API initialization
                    val (initResponse, abTestResponse) = ApiRequests.initialize(sdkKey = sdkKey, userId = userId)
                    
                    // Store initialization data
                    this@MonetaiSDK.organizationId = initResponse.organization_id
                    this@MonetaiSDK.abTestGroup = abTestResponse.group
                    this@MonetaiSDK.campaign = abTestResponse.campaign
                    this@MonetaiSDK.exposureTimeSec = abTestResponse.campaign?.exposureTimeSec
                    
                    // Initialization complete
                    isInitialized = true
                    
                    // Process pending events
                    Log.d(TAG, "SDK initialization complete - Starting to process pending events...")
                    processPendingEvents()
                    Log.d(TAG, "Pending events processing complete")
                    
                    // Automatically check discount information after initialization
                    loadDiscountInfoAutomatically()
                    
                    InitializeResult(
                        organizationId = initResponse.organization_id,
                        platform = initResponse.platform,
                        version = initResponse.version,
                        userId = userId,
                        group = abTestResponse.group
                    )
                }
                
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
     * Automatically load discount information and update state
     */
    private suspend fun loadDiscountInfoAutomatically() {
        val sdkKey = sdkKey ?: return
        val userId = userId ?: return
        
        try {
            val discount = ApiRequests.getAppUserDiscount(sdkKey = sdkKey, userId = userId)
            
            // Check if discount information belongs to current user
            if (discount != null && discount.appUserId != userId) {
                return
            }
            
            // Update state
            currentDiscount = discount
            
            // Call callback if set
            onDiscountInfoChange?.invoke(discount)
            
            Log.d(TAG, "Discount information auto-load complete: ${if (discount != null) "Discount available" else "No discount"}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Discount information auto-load failed", e)
            currentDiscount = null
            onDiscountInfoChange?.invoke(null)
        }
    }
    
    /**
     * Log event (using LogEventOptions)
     * @param options Event options to log
     */
    fun logEvent(options: LogEventOptions) {
        Log.d(TAG, "Event logging request: ${options.eventName}")
        Log.d(TAG, "Event parameters: ${options.params ?: emptyMap()}")
        
        val sdkKey = sdkKey
        val userId = userId
        
        if (sdkKey == null || userId == null) {
            // Add to queue if SDK is not initialized
            pendingEvents.offer(options)
            Log.d(TAG, "Before SDK initialization - Added to queue: ${options.eventName}")
            Log.d(TAG, "Current number of pending events: ${pendingEvents.size}")
            return
        }
        
        Log.d(TAG, "SDK initialized - Sending immediately: ${options.eventName}")
        
        internalScope.launch {
            try {
                ApiRequests.createEvent(
                    sdkKey = sdkKey,
                    userId = userId,
                    eventName = options.eventName,
                    params = options.params,
                    createdAt = options.createdAt
                )
                Log.d(TAG, "Event logging success: ${options.eventName}")
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
     * Perform user prediction
     * @param completion Completion callback with result or error
     */
    fun predict(completion: ((PredictResponse?, Exception?) -> Unit)? = null) {
        internalScope.launch {
            try {
                val sdkKey = sdkKey ?: throw MonetaiError.NotInitialized
                val userId = userId ?: throw MonetaiError.NotInitialized
                val exposureTimeSec = exposureTimeSec ?: throw MonetaiError.NotInitialized
                
                val result = ApiRequests.predict(sdkKey = sdkKey, userId = userId)
                
                // Create discount for non-purchaser prediction (only if prediction is not null)
                val prediction = PredictResult.fromString(result.prediction)
                if (prediction == PredictResult.NON_PURCHASER) {
                    handleNonPurchaserPrediction(sdkKey = sdkKey, userId = userId, exposureTimeSec = exposureTimeSec)
                }
                
                val predictResponse = result.toPredictResponse()
                
                withContext(Dispatchers.Main) {
                    completion?.invoke(predictResponse, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Prediction failed", e)
                withContext(Dispatchers.Main) {
                    completion?.invoke(null, e)
                }
            }
        }
    }
    
    /**
     * Get current user's discount information
     * @param completion Completion callback with discount or error
     */
    fun getCurrentDiscount(completion: ((AppUserDiscount?, Exception?) -> Unit)? = null) {
        internalScope.launch {
            try {
                val sdkKey = sdkKey ?: throw MonetaiError.NotInitialized
                val userId = userId ?: throw MonetaiError.NotInitialized
                
                val discount = ApiRequests.getAppUserDiscount(sdkKey = sdkKey, userId = userId)
                
                withContext(Dispatchers.Main) {
                    completion?.invoke(discount, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get current discount", e)
                withContext(Dispatchers.Main) {
                    completion?.invoke(null, e)
                }
            }
        }
    }
    
    /**
     * Check if active discount exists
     * @param completion Completion callback with result or error
     */
    fun hasActiveDiscount(completion: ((Boolean, Exception?) -> Unit)? = null) {
        internalScope.launch {
            try {
                val discount = getCurrentDiscountInternal()
                val hasDiscount = discount != null && discount.endedAt > Date()
                
                withContext(Dispatchers.Main) {
                    completion?.invoke(hasDiscount, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check active discount", e)
                withContext(Dispatchers.Main) {
                    completion?.invoke(false, e)
                }
            }
        }
    }
    
    /**
     * Reset SDK
     */
    fun reset() {
        sdkKey = null
        userId = null
        campaign = null
        exposureTimeSec = null
        organizationId = null
        abTestGroup = null
        isInitialized = false
        pendingEvents.clear()
        currentDiscount = null
        
        // Clear SharedPreferences
        sharedPreferences?.edit()
            ?.remove("MonetaiSdkKey")
            ?.remove("MonetaiAppAccountToken")
            ?.apply()
        
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
     * Return current exposure time (seconds)
     */
    fun getExposureTimeSec(): Int? = exposureTimeSec
    
    // MARK: - Private Methods
    
    /**
     * Internal method to get current discount (for internal use only)
     */
    private suspend fun getCurrentDiscountInternal(): AppUserDiscount? {
        val sdkKey = sdkKey ?: throw MonetaiError.NotInitialized
        val userId = userId ?: throw MonetaiError.NotInitialized
        
        return ApiRequests.getAppUserDiscount(sdkKey = sdkKey, userId = userId)
    }
    
    private suspend fun processPendingEvents() {
        val sdkKey = sdkKey ?: return
        val userId = userId ?: return
        
        val events = mutableListOf<LogEventOptions>()
        while (pendingEvents.isNotEmpty()) {
            pendingEvents.poll()?.let { events.add(it) }
        }
        
        Log.d(TAG, "Starting to process pending events")
        Log.d(TAG, "Number of events to process: ${events.size}")
        
        if (events.isEmpty()) {
            Log.d(TAG, "No pending events to process")
            return
        }
        
        Log.d(TAG, "List of events to process: ${events.map { it.eventName }}")
        
        events.forEachIndexed { index, event ->
            Log.d(TAG, "Processing ${index + 1}/${events.size}: ${event.eventName}")
            
            try {
                ApiRequests.createEvent(
                    sdkKey = sdkKey,
                    userId = userId,
                    eventName = event.eventName,
                    params = event.params,
                    createdAt = event.createdAt
                )
                Log.d(TAG, "${index + 1}/${events.size} Success: ${event.eventName}")
            } catch (e: Exception) {
                Log.e(TAG, "${index + 1}/${events.size} Failed: ${event.eventName}", e)
            }
        }
    }
    
    private suspend fun handleNonPurchaserPrediction(sdkKey: String, userId: String, exposureTimeSec: Int) {
        try {
            // Check existing discount information
            val existingDiscount = ApiRequests.getAppUserDiscount(sdkKey = sdkKey, userId = userId)
            
            val now = Date()
            val hasActiveDiscount = existingDiscount != null && existingDiscount.endedAt > now
            
            if (!hasActiveDiscount) {
                // Create new discount
                val startedAt = now
                val calendar = Calendar.getInstance()
                calendar.time = startedAt
                calendar.add(Calendar.SECOND, exposureTimeSec)
                val endedAt = calendar.time
                
                val discount = ApiRequests.createAppUserDiscount(
                    sdkKey = sdkKey,
                    userId = userId,
                    startedAt = startedAt,
                    endedAt = endedAt
                )
                
                // Update state
                currentDiscount = discount
                
                // Call callback
                onDiscountInfoChange?.invoke(discount)
                
                Log.d(TAG, "Non-purchaser discount created successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create non-purchaser discount", e)
        }
    }
} 