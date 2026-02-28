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
    }

    /**
     * Java-friendly callback interfaces
     */
    @FunctionalInterface
    interface InitializeCallback {
        fun onResult(result: InitializeResult?, error: Exception?)
    }

    @FunctionalInterface
    interface OfferCallback {
        fun onResult(offer: Offer?, error: Exception?)
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
     * Get dynamic pricing offer (Java compatible)
     * @param promotionId Promotion ID
     * @param completion Callback with offer or error
     */
    fun getOffer(promotionId: Int, completion: OfferCallback? = null) {
        MonetaiSDK.shared.getOffer(promotionId) { offer, error ->
            completion?.onResult(offer, error)
        }
    }

    /**
     * Log view product item event (Java compatible)
     * @param params View product item parameters
     */
    fun logViewProductItem(params: ViewProductItemParams) {
        MonetaiSDK.shared.logViewProductItem(params)
    }

    /**
     * Reset SDK (Java compatible)
     */
    fun reset() {
        MonetaiSDK.shared.reset()
    }
}
