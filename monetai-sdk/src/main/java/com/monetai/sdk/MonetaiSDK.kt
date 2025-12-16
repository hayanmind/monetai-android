package com.monetai.sdk

import android.content.Context
import android.util.Log
import com.monetai.sdk.billing.BillingManager
import com.monetai.sdk.billing.ReceiptValidator
import com.monetai.sdk.models.*
import com.monetai.sdk.network.ApiRequests
import com.monetai.sdk.paywall.MonetaiPaywallManager
import com.monetai.sdk.banner.MonetaiBannerManager
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
    
    // Paywall Configuration
    private var paywallConfig: PaywallConfig? = null
    
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
    
    // MARK: - Subscription State
    private var isSubscriber: Boolean = false
    
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
                    this@MonetaiSDK.applicationContext = context // Store application context

                    // Start billing observation (BillingClient requires main thread)
                    billingManager = BillingManager(context, sdkKey, userId)
                    billingManager?.startObserving()
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
                val (initResponse, abTestResponse) = ApiRequests.initialize(sdkKey = sdkKey, userId = userId)

                // Store initialization data (IO)
                this@MonetaiSDK.organizationId = initResponse.organization_id
                this@MonetaiSDK.abTestGroup = abTestResponse.group
                this@MonetaiSDK.campaign = abTestResponse.campaign
                this@MonetaiSDK.exposureTimeSec = abTestResponse.campaign?.exposureTimeSec

                // Initialization complete (IO)
                isInitialized = true

                // Process pending events (IO)
                processPendingEvents()

                // Automatically check discount information after initialization (IO)
                loadDiscountInfoAutomatically()

                val result = InitializeResult(
                    organizationId = initResponse.organization_id,
                    platform = initResponse.platform,
                    version = initResponse.version,
                    userId = userId,
                    group = abTestResponse.group
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

            // Call callback on main thread if set
            withContext(Dispatchers.Main) {
                onDiscountInfoChange?.invoke(discount)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Discount information auto-load failed", e)
            currentDiscount = null
            withContext(Dispatchers.Main) {
                onDiscountInfoChange?.invoke(null)
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
            // Add to queue if SDK is not initialized
            pendingEvents.offer(options)
            return
        }
        
        internalScope.launch {
            try {
                ApiRequests.createEvent(
                    sdkKey = sdkKey,
                    userId = userId,
                    eventName = options.eventName,
                    params = options.params,
                    createdAt = options.createdAt
                )
            } catch (e: Exception) {
                Log.e(TAG, "Event logging failed: ${options.eventName}", e)
            }
        }
    }
    
    /**
     * Log view product item event (dedicated endpoint)
     * @param params Event parameters
     */
    fun logViewProductItem(params: ViewProductItemParams) {
        val sdkKey = sdkKey
        val userId = userId
        
        if (sdkKey == null || userId == null) {
            Log.w(TAG, "logViewProductItem called before SDK initialization; event ignored.")
            return
        }
        
        internalScope.launch {
            try {
                ApiRequests.logViewProductItem(
                    sdkKey = sdkKey,
                    userId = userId,
                    params = params,
                    createdAt = Date()
                )
            } catch (e: Exception) {
                Log.e(TAG, "logViewProductItem failed", e)
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
    
    /**
     * Set subscription status (can be called before or after configurePaywall)
     * @param isSubscriber Whether the user is currently a subscriber
     */
    fun setSubscriptionStatus(isSubscriber: Boolean) {
        this.isSubscriber = isSubscriber
        
        // SDK automatically updates all UI when subscription status changes
        if (paywallConfig != null) {
            configureManagersAndUpdateUI()
        }
    }
    
    /**
     * Get current subscription status
     */
    fun getSubscriptionStatus(): Boolean = isSubscriber
    
    /**
     * Get application context
     */
    fun getApplicationContext(): Context? = applicationContext
    
    /**
     * Configure paywall with configuration
     * @param config Paywall configuration
     */
    fun configurePaywall(config: PaywallConfig) {
        this.paywallConfig = config
        
        // SDK automatically configures managers and updates UI
        configureManagersAndUpdateUI()
    }
    
    
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
        
        if (events.isEmpty()) {
            return
        }
        
        events.forEach { event ->
            try {
                ApiRequests.createEvent(
                    sdkKey = sdkKey,
                    userId = userId,
                    eventName = event.eventName,
                    params = event.params,
                    createdAt = event.createdAt
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process event: ${event.eventName}", e)
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
                
                // Update state and UI automatically
                handleDiscountInfoChange(discount)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create non-purchaser discount", e)
        }
    }
    
    // MARK: - Paywall and Banner Management
    
    /**
     * Configure managers and update UI automatically
     */
    private fun configureManagersAndUpdateUI() {
        val config = paywallConfig ?: return
        
        // 1. Configure paywall manager
        paywallManager.configure(config, convertToDiscountInfo())
        
        // 2. Configure banner manager (no rootView needed for window overlay)
        bannerManager.configure(config, convertToDiscountInfo(), paywallManager)
        
        // 3. Automatically update banner visibility
        updateBannerVisibilityAutomatically()
        
        // 4. Paywall will be preloaded when banner is shown (better timing)
    }
    
    /**
     * Update banner visibility automatically based on subscription status
     */
    private fun updateBannerVisibilityAutomatically() {
        // Hide banner if user is subscriber
        if (isSubscriber) {
            bannerManager.hideBanner()
        } else {
            // Show banner if discount is active and paywall is enabled
            val shouldShow = shouldShowBanner()
            
            if (shouldShow) {
                bannerManager.showBanner()
            }
        }
    }
    
    /**
     * Check if banner should be shown
     */
    private fun shouldShowBanner(): Boolean {
        return currentDiscount?.let { discount ->
            discount.endedAt > Date() && paywallConfig?.enabled == true
        } ?: false
    }
    
    /**
     * Check if paywall should be shown automatically
     */
    private fun shouldShowPaywall(): Boolean {
        return currentDiscount?.let { discount ->
            discount.endedAt > Date() && paywallConfig?.enabled == true && !isSubscriber
        } ?: false
    }
    
    /**
     * Convert AppUserDiscount to DiscountInfo
     */
    private fun convertToDiscountInfo(): DiscountInfo? {
        val currentDiscount = currentDiscount ?: return null
        
        return DiscountInfo(
            startedAt = currentDiscount.startedAt,
            endedAt = currentDiscount.endedAt,
            userId = currentDiscount.appUserId,
            sdkKey = currentDiscount.sdkKey
        )
    }
    
    /**
     * Handle discount info change and update UI automatically
     */
    private fun handleDiscountInfoChange(discount: AppUserDiscount?) {
        currentDiscount = discount
        
        // Switch to main thread for UI updates
        internalScope.launch(Dispatchers.Main) {
            // Update managers with new discount info
            if (paywallConfig != null) {
                configureManagersAndUpdateUI()
            }
            
            // Call external callback
            onDiscountInfoChange?.invoke(discount)
        }
    }
} 