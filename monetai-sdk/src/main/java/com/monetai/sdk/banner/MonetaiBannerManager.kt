package com.monetai.sdk.banner

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.monetai.sdk.models.BannerParams
import com.monetai.sdk.models.DiscountInfo
import com.monetai.sdk.models.PaywallConfig
import com.monetai.sdk.paywall.MonetaiPaywallManager
import com.monetai.sdk.utils.BannerStyleUtils
import com.monetai.sdk.utils.MonetaiPromotionTimer
import com.monetai.sdk.utils.MonetaiPromotionTimerDelegate
import com.monetai.sdk.utils.ThreadUtils
import com.monetai.sdk.utils.ContextUtils
import com.monetai.sdk.utils.DimensionUtils
import java.util.*

/**
 * BannerManager handles the display and management of banner UI
 * Includes expiration management via promotion timer
 */
class MonetaiBannerManager(private val context: Context) : MonetaiPromotionTimerDelegate {

    companion object {
        private const val TAG = "MonetaiBannerManager"
    }

    // MARK: - Published Properties
    private val _bannerVisible = MutableLiveData<Boolean>(false)
    val bannerVisible: LiveData<Boolean> = _bannerVisible

    private val _bannerParams = MutableLiveData<BannerParams?>()
    val bannerParams: LiveData<BannerParams?> = _bannerParams

    // MARK: - Private Properties
    private var paywallConfig: PaywallConfig? = null
    private var discountInfo: DiscountInfo? = null
    private var paywallManager: MonetaiPaywallManager? = null
    private var bannerView: MonetaiBannerView? = null

    // MARK: - Promotion Expiration Management
    private var promotionTimer: MonetaiPromotionTimer? = null

    init {
        setupPromotionExpirationManager()
    }

    // MARK: - Promotion Expiration Setup
    private fun setupPromotionExpirationManager() {
        promotionTimer = MonetaiPromotionTimer(context, this)
    }

    // MARK: - MonetaiPromotionTimerDelegate
    override fun promotionDidExpire() {
        hideBanner()
    }

    // MARK: - Public Methods

    /**
     * Configure banner with configuration and discount info
     */
    fun configure(
        paywallConfig: PaywallConfig,
        discountInfo: DiscountInfo?,
        paywallManager: MonetaiPaywallManager
    ) {
        this.paywallConfig = paywallConfig
        this.discountInfo = discountInfo
        this.paywallManager = paywallManager

        updateBannerParams()

        // Configure promotion expiration manager
        discountInfo?.let { info ->
            promotionTimer?.configure(info)
        }
    }

    /**
     * Show banner
     */
    fun showBanner() {
        if (_bannerParams.value == null) {
            Log.w(TAG, "Cannot show banner - bannerParams is null")
            return
        }

        ThreadUtils.runOnMainThread {
            _bannerVisible.value = true
            createAndShowBannerView()
            promotionTimer?.startMonitoring()
        }
    }

    /**
     * Hide banner
     */
    fun hideBanner() {
        ThreadUtils.runOnMainThread {
            _bannerVisible.value = false
            removeBannerView()
            promotionTimer?.stopMonitoring()
        }
    }

    /**
     * Cleanup resources when banner manager is no longer needed
     */
    fun destroy() {
        hideBanner()
        promotionTimer?.destroy()
        promotionTimer = null
    }

    // MARK: - Private Methods

    private fun updateBannerParams() {
        val paywallConfig = paywallConfig ?: return
        val discountInfo = discountInfo ?: return

        val params = BannerParams(
            locale = paywallConfig.locale,
            discountPercent = paywallConfig.discountPercent,
            endedAt = discountInfo.endedAt,
            style = paywallConfig.style,
            bottom = paywallConfig.bannerBottom
        )

        ThreadUtils.runOnMainThread {
            _bannerParams.value = params
        }
    }

    private fun createAndShowBannerView() {
        val bannerParams = _bannerParams.value ?: return

        // Create banner view if not exists
        if (bannerView == null) {
            bannerView = MonetaiBannerView(context).apply {
                setBannerParams(bannerParams)
                setOnPaywallClickListener {
                    paywallManager?.showPaywall()
                }
            }

            // Calculate banner dimensions
            val requestedBottomMargin = DimensionUtils.dpToPx(context, bannerParams.bottom)
            val bannerHeightDp = BannerStyleUtils.getBannerHeight(bannerParams.style)
            val bannerHeightPx = DimensionUtils.dpToPx(context, bannerHeightDp)
            val horizontalMarginDp = BannerStyleUtils.getBannerHorizontalMargin(bannerParams.style)
            val horizontalMarginPx = DimensionUtils.dpToPx(context, horizontalMarginDp)

            // Find activity's root view (DecorView)
            val activity = ContextUtils.findActivity(context)
            if (activity != null) {
                val decorView = activity.window.decorView as ViewGroup

                // Calculate bottom margin relative to NavigationBar (like iOS safe area)
                val windowInsets = ViewCompat.getRootWindowInsets(decorView)
                val navBarHeight = windowInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
                val bottomMarginPx = navBarHeight + requestedBottomMargin

                // Simple FrameLayout params for bottom positioning with horizontal margins
                val layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    bannerHeightPx,
                    android.view.Gravity.BOTTOM
                ).apply {
                    setMargins(horizontalMarginPx, 0, horizontalMarginPx, bottomMarginPx)
                }

                // Add banner to activity's decor view
                decorView.addView(bannerView, layoutParams)
            } else {
                Log.e(TAG, "Cannot find current activity")
                return
            }
        }

        // Show banner view
        bannerView?.show()
    }

    private fun removeBannerView() {
        bannerView?.let { banner ->
            banner.hide()

            // Remove from activity's decor view
            try {
                val activity = ContextUtils.findActivity(context)
                if (activity != null) {
                    val decorView = activity.window.decorView as ViewGroup
                    decorView.removeView(banner)
                } else {
                    Log.w(TAG, "Cannot find current activity to remove banner")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove banner: ${e.message}")
            }
        }
        bannerView = null
    }
}
