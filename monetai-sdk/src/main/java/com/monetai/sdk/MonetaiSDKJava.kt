package com.monetai.sdk

import android.content.Context
import com.monetai.sdk.models.*

/**
 * Java-compatible wrapper for MonetaiSDK
 * Provides callback-based methods for Java developers
 */
class MonetaiSDKJava {
    
    companion object {
        private val _shared: MonetaiSDKJava by lazy { MonetaiSDKJava() }
        
        @JvmStatic
        fun getShared(): MonetaiSDKJava = _shared
        
        @JvmStatic
        fun getUserId(): String? = MonetaiSDK.shared.getUserId()
        
        @JvmStatic
        fun getSdkKey(): String? = MonetaiSDK.shared.getSdkKey()
        
        @JvmStatic
        fun getInitialized(): Boolean = MonetaiSDK.shared.getInitialized()
        
        @JvmStatic
        fun getExposureTimeSec(): Int? = MonetaiSDK.shared.getExposureTimeSec()
        
        /**
         * Set subscription status (Java compatible)
         * @param isSubscriber Whether the user is currently a subscriber
         */
        @JvmStatic
        fun setSubscriptionStatus(isSubscriber: Boolean) {
            MonetaiSDK.shared.setSubscriptionStatus(isSubscriber)
        }
        
        /**
         * Get current subscription status (Java compatible)
         * @return Current subscription status
         */
        @JvmStatic
        fun getSubscriptionStatus(): Boolean {
            return MonetaiSDK.shared.getSubscriptionStatus()
        }
        
        /**
         * Configure paywall with configuration (Java compatible)
         * @param config Paywall configuration
         */
        @JvmStatic
        fun configurePaywall(config: PaywallConfig) {
            MonetaiSDK.shared.configurePaywall(config)
        }
        
        
        /**
         * Create PaywallConfigBuilder (Java compatible)
         * @return PaywallConfigBuilder instance
         */
        @JvmStatic
        fun createPaywallConfigBuilder(): PaywallConfigBuilder {
            return PaywallConfigBuilder()
        }
    }
    
    /**
     * Java-friendly callback interfaces
     */
    @FunctionalInterface
    interface InitializeCallback {
        fun onResult(result: InitializeResult?, error: Exception?)
    }
    
    @FunctionalInterface
    interface PredictCallback {
        fun onResult(result: PredictResponse?, error: Exception?)
    }
    
    @FunctionalInterface
    interface DiscountCallback {
        fun onResult(result: AppUserDiscount?, error: Exception?)
    }
    
    @FunctionalInterface
    interface BooleanCallback {
        fun onResult(result: Boolean, error: Exception?)
    }
    
    @FunctionalInterface
    interface DiscountInfoChangeCallback {
        fun onDiscountChanged(discount: AppUserDiscount?)
    }
    
    /**
     * Initialize MonetaiSDK (Java compatible)
     */
    @JvmOverloads
    fun initialize(
        context: Context,
        sdkKey: String,
        userId: String,
        completion: InitializeCallback? = null
    ) {
        MonetaiSDK.shared.initialize(context, sdkKey, userId) { result, error ->
            completion?.onResult(result, error)
        }
    }
    
    /**
     * Log event (Java compatible)
     */
    @JvmOverloads
    fun logEvent(
        eventName: String,
        params: Map<String, Any>? = null
    ) {
        MonetaiSDK.shared.logEvent(eventName, params)
    }
    
    /**
     * Log event with options (Java compatible)
     */
    fun logEvent(options: LogEventOptions) {
        MonetaiSDK.shared.logEvent(options)
    }
    
    /**
     * Perform user prediction (Java compatible)
     */
    fun predict(completion: PredictCallback? = null) {
        MonetaiSDK.shared.predict { result, error ->
            completion?.onResult(result, error)
        }
    }
    
    /**
     * Get current user's discount information (Java compatible)
     */
    fun getCurrentDiscount(completion: DiscountCallback? = null) {
        MonetaiSDK.shared.getCurrentDiscount { result, error ->
            completion?.onResult(result, error)
        }
    }
    
    /**
     * Check if active discount exists (Java compatible)
     */
    fun hasActiveDiscount(completion: BooleanCallback? = null) {
        MonetaiSDK.shared.hasActiveDiscount { result, error ->
            completion?.onResult(result, error)
        }
    }
    
    /**
     * Set discount info change callback (Java compatible)
     */
    fun setOnDiscountInfoChange(callback: DiscountInfoChangeCallback?) {
        MonetaiSDK.shared.onDiscountInfoChange = callback?.let { cb ->
            { discount -> cb.onDiscountChanged(discount) }
        }
    }
    
    /**
     * Remove discount info change callback (Java compatible)
     */
    fun removeOnDiscountInfoChange() {
        MonetaiSDK.shared.onDiscountInfoChange = null
    }
    
    /**
     * Reset SDK (Java compatible)
     */
    fun reset() {
        MonetaiSDK.shared.reset()
    }
    
    /**
     * Java-compatible callback interfaces for PaywallConfig
     */
    @FunctionalInterface
    interface OnPurchaseCallback {
        fun onPurchase(context: PaywallContext, closePaywall: Runnable)
    }
    
    @FunctionalInterface  
    interface OnTermsOfServiceCallback {
        fun onTermsOfService(context: PaywallContext)
    }
    
    @FunctionalInterface
    interface OnPrivacyPolicyCallback {
        fun onPrivacyPolicy(context: PaywallContext)
    }
    
    /**
     * Builder class for PaywallConfig (Java compatible)
     * Provides the same functionality as iOS and React Native SDKs
     */
    class PaywallConfigBuilder {
        private var discountPercent: Int = 0
        private var regularPrice: String = ""
        private var discountedPrice: String = ""
        private var locale: String = ""
        private var style: PaywallStyle = PaywallStyle.TEXT_FOCUSED
        private var features: List<Feature> = emptyList()
        private var enabled: Boolean = true
        private var bannerBottom: Float = 20f
        private var isSubscriber: Boolean = false
        
        
        private var onPurchase: ((PaywallContext, (() -> Unit)) -> Unit)? = null
        private var onTermsOfService: ((PaywallContext) -> Unit)? = null
        private var onPrivacyPolicy: ((PaywallContext) -> Unit)? = null
        
        // Java-compatible callback holders
        private var onPurchaseJava: OnPurchaseCallback? = null
        private var onTermsOfServiceJava: OnTermsOfServiceCallback? = null
        private var onPrivacyPolicyJava: OnPrivacyPolicyCallback? = null
        
        fun discountPercent(value: Int): PaywallConfigBuilder {
            this.discountPercent = value
            return this
        }
        
        fun regularPrice(value: String): PaywallConfigBuilder {
            this.regularPrice = value
            return this
        }
        
        fun discountedPrice(value: String): PaywallConfigBuilder {
            this.discountedPrice = value
            return this
        }
        
        fun locale(value: String): PaywallConfigBuilder {
            this.locale = value
            return this
        }
        
        fun style(value: PaywallStyle): PaywallConfigBuilder {
            this.style = value
            return this
        }
        
        fun features(value: List<Feature>): PaywallConfigBuilder {
            this.features = value
            return this
        }
        
        fun enabled(value: Boolean): PaywallConfigBuilder {
            this.enabled = value
            return this
        }
        
        fun bannerBottom(value: Float): PaywallConfigBuilder {
            this.bannerBottom = value
            return this
        }
        
        fun isSubscriber(value: Boolean): PaywallConfigBuilder {
            this.isSubscriber = value
            return this
        }
        
        
        fun onPurchase(value: ((PaywallContext, (() -> Unit)) -> Unit)?): PaywallConfigBuilder {
            this.onPurchase = value
            return this
        }
        
        fun onTermsOfService(value: ((PaywallContext) -> Unit)?): PaywallConfigBuilder {
            this.onTermsOfService = value
            return this
        }
        
        fun onPrivacyPolicy(value: ((PaywallContext) -> Unit)?): PaywallConfigBuilder {
            this.onPrivacyPolicy = value
            return this
        }
        
        // Java-compatible callback setters
        fun onPurchase(value: OnPurchaseCallback?): PaywallConfigBuilder {
            this.onPurchaseJava = value
            return this
        }
        
        fun onTermsOfService(value: OnTermsOfServiceCallback?): PaywallConfigBuilder {
            this.onTermsOfServiceJava = value
            return this
        }
        
        fun onPrivacyPolicy(value: OnPrivacyPolicyCallback?): PaywallConfigBuilder {
            this.onPrivacyPolicyJava = value
            return this
        }
        
        fun build(): PaywallConfig {
            // Convert Java callbacks to Kotlin functions if Java callbacks are provided
            val kotlinOnPurchase = onPurchaseJava?.let { javaCallback ->
                { context: PaywallContext, closePaywall: () -> Unit ->
                    javaCallback.onPurchase(context, Runnable { closePaywall() })
                }
            } ?: onPurchase
            
            val kotlinOnTermsOfService = onTermsOfServiceJava?.let { javaCallback ->
                { context: PaywallContext -> javaCallback.onTermsOfService(context) }
            } ?: onTermsOfService
            
            val kotlinOnPrivacyPolicy = onPrivacyPolicyJava?.let { javaCallback ->
                { context: PaywallContext -> javaCallback.onPrivacyPolicy(context) }
            } ?: onPrivacyPolicy
            
            return PaywallConfig(
                discountPercent = discountPercent,
                regularPrice = regularPrice,
                discountedPrice = discountedPrice,
                locale = locale,
                style = style,
                features = features,
                enabled = enabled,
                bannerBottom = bannerBottom,
                isSubscriber = isSubscriber,
                onPurchase = kotlinOnPurchase,
                onTermsOfService = kotlinOnTermsOfService,
                onPrivacyPolicy = kotlinOnPrivacyPolicy
            )
        }
    }
}