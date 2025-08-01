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
} 