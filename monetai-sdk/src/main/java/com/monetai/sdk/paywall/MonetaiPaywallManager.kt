package com.monetai.sdk.paywall

import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.monetai.sdk.models.DiscountInfo
import com.monetai.sdk.models.PaywallConfig
import com.monetai.sdk.models.PaywallParams
import com.monetai.sdk.utils.DateTimeHelper
import com.monetai.sdk.utils.ThreadUtils
import com.monetai.sdk.utils.ContextUtils

/**
 * PaywallManager handles the display and management of paywall UI
 * Provides manual control for paywall presentation
 */
class MonetaiPaywallManager {

    companion object {
        private const val TAG = "MonetaiPaywallManager"
    }

    // MARK: - Published Properties
    private val _paywallVisible = MutableLiveData<Boolean>(false)
    val paywallVisible: LiveData<Boolean> = _paywallVisible

    private val _paywallParams = MutableLiveData<PaywallParams?>()
    val paywallParams: LiveData<PaywallParams?> = _paywallParams

    // MARK: - Private Properties
    private var paywallConfig: PaywallConfig? = null
    private var discountInfo: DiscountInfo? = null
    private var currentPaywallActivity: MonetaiPaywallActivity? = null

    // MARK: - Public Methods

    /**
     * Configure paywall with configuration and discount info
     */
    fun configure(paywallConfig: PaywallConfig, discountInfo: DiscountInfo?) {
        this.paywallConfig = paywallConfig
        this.discountInfo = discountInfo

        updatePaywallParams()
    }

    /**
     * Show paywall
     */
    fun showPaywall() {
        // Ensure we're on the main thread for UI operations
        if (!ThreadUtils.isMainThread()) {
            ThreadUtils.runOnMainThread {
                showPaywall()
            }
            return
        }

        if (_paywallParams.value == null) {
            updatePaywallParams()

            if (_paywallParams.value == null) {
                Log.e(TAG, "Cannot show paywall - paywallParams still null after update")
                return
            }
        }

        ThreadUtils.runOnMainThread {
            _paywallVisible.value = true
            presentPaywall()
        }
    }

    /**
     * Hide paywall
     */
    fun hidePaywall() {
        ThreadUtils.runOnMainThread {
            _paywallVisible.value = false
            dismissPaywall()
        }
    }

    /**
     * Handle purchase action
     */
    internal fun handlePurchase(paywallContext: com.monetai.sdk.models.PaywallContext, closePaywall: (() -> Unit)) {
        val onPurchase = paywallConfig?.onPurchase
        if (onPurchase != null) {
            onPurchase(paywallContext, closePaywall)
        } else {
            Log.w(TAG, "onPurchase callback not set")
        }
    }

    /**
     * Handle terms of service action
     */
    internal fun handleTermsOfService(paywallContext: com.monetai.sdk.models.PaywallContext) {
        val callback = paywallConfig?.onTermsOfService
        if (callback != null) {
            callback.invoke(paywallContext)
        } else {
            Log.w(TAG, "onTermsOfService callback not set")
        }
    }

    /**
     * Handle privacy policy action
     */
    internal fun handlePrivacyPolicy(paywallContext: com.monetai.sdk.models.PaywallContext) {
        val callback = paywallConfig?.onPrivacyPolicy
        if (callback != null) {
            callback.invoke(paywallContext)
        } else {
            Log.w(TAG, "onPrivacyPolicy callback not set")
        }
    }

    // MARK: - Private Methods

    private fun updatePaywallParams() {
        val paywallConfig = paywallConfig ?: return
        val discountInfo = discountInfo ?: return

        val params = PaywallParams(
            discountPercent = paywallConfig.discountPercent.toString(),
            endedAt = DateTimeHelper.formatToISO8601(discountInfo.endedAt),
            regularPrice = paywallConfig.regularPrice,
            discountedPrice = paywallConfig.discountedPrice,
            locale = paywallConfig.locale,
            features = paywallConfig.features,
            style = paywallConfig.style
        )

        ThreadUtils.runOnMainThread {
            _paywallParams.value = params
        }
    }

    // MARK: - Paywall Presentation
    private fun presentPaywall() {
        val paywallParams = _paywallParams.value ?: run {
            Log.e(TAG, "paywallParams is null in presentPaywall()")
            return
        }

        val topActivity = ContextUtils.findActivity(null) ?: run {
            Log.e(TAG, "Cannot find top activity")
            return
        }

        // Avoid double-present
        if (currentPaywallActivity != null) {
            Log.w(TAG, "Paywall already presented")
            return
        }

        // Create PaywallActivity
        val paywallActivity = MonetaiPaywallActivity()

        // Set static callbacks
        MonetaiPaywallActivity.setStaticCallbacks(
            onClose = { hidePaywall() },
            onPurchase = { paywallContext, closePaywall -> handlePurchase(paywallContext, closePaywall) },
            onTermsOfService = { paywallContext -> handleTermsOfService(paywallContext) },
            onPrivacyPolicy = { paywallContext -> handlePrivacyPolicy(paywallContext) }
        )

        // Also set instance callbacks
        paywallActivity.setCallbacks(
            onClose = { hidePaywall() },
            onPurchase = { paywallContext, closePaywall -> handlePurchase(paywallContext, closePaywall) },
            onTermsOfService = { paywallContext -> handleTermsOfService(paywallContext) },
            onPrivacyPolicy = { paywallContext -> handlePrivacyPolicy(paywallContext) }
        )

        // Present paywall
        val intent = Intent(topActivity, MonetaiPaywallActivity::class.java).apply {
            putExtra("paywallParams", paywallParams)
        }

        try {
            topActivity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start activity", e)
            return
        }

        currentPaywallActivity = paywallActivity
    }

    private fun dismissPaywall() {
        currentPaywallActivity?.finish()
        currentPaywallActivity = null
    }
}
