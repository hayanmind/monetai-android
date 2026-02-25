package com.monetai.sdk.banner

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.monetai.sdk.constants.WebViewConstants
import com.monetai.sdk.models.BannerParams
import com.monetai.sdk.models.PaywallStyle
import com.monetai.sdk.utils.BannerStyleUtils
import com.monetai.sdk.utils.DateTimeHelper
import com.monetai.sdk.utils.DimensionUtils

/**
 * BannerView displays the banner using WebView
 */
class MonetaiBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    companion object {
        private const val TAG = "MonetaiBannerView"
    }
    
    
    // MARK: - Properties
    private var bannerParams: BannerParams? = null
    private var onPaywallClickListener: (() -> Unit)? = null
    private lateinit var webView: WebView
    
    // MARK: - Initialization
    
    init {
        // setupWebView() will be called when setBannerParams() is called
        // to ensure bannerParams is available for proper dimension calculation
    }
    
    // MARK: - Public Methods
    
    fun setBannerParams(params: BannerParams) {
        this.bannerParams = params
        // Re-setup WebView with new banner params to ensure correct dimensions
        setupWebView()
        loadBanner()
    }
    
    fun setOnPaywallClickListener(listener: (() -> Unit)?) {
        this.onPaywallClickListener = listener
    }
    
    fun show() {
        visibility = View.VISIBLE
        requestLayout()
        invalidate()
    }
    
    fun hide() {
        visibility = View.GONE
    }
    
    // MARK: - Private Methods
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        // Set banner dimensions based on style
        val style = bannerParams?.style ?: PaywallStyle.KEY_FEATURE_SUMMARY
        val bannerHeightDp = BannerStyleUtils.getBannerHeight(style)
        val heightPx = DimensionUtils.dpToPx(context, bannerHeightDp)

        // Set banner view size using ViewGroup.LayoutParams
        var newLayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            heightPx
        )

        // Set banner position (bottom)
        val bannerParams = bannerParams
        if (bannerParams != null) {
            val bottomMarginPx = DimensionUtils.dpToPx(context, bannerParams.bottom)
            val marginLayoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx
            )
            marginLayoutParams.bottomMargin = bottomMarginPx
            newLayoutParams = marginLayoutParams
        }

        layoutParams = newLayoutParams

        // Apply corner radius based on style (matching iOS implementation)
        val cornerRadiusDp = BannerStyleUtils.getBannerCornerRadius(style)
        val cornerRadiusPx = DimensionUtils.dpToPx(context, cornerRadiusDp)
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.TRANSPARENT)
            setCornerRadius(cornerRadiusPx.toFloat())
        }
        clipToOutline = true

        webView = WebView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )

            // Hide WebView initially to prevent flickering
            alpha = 0f

            // WebView settings
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = false
                setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
            }

            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            settings.userAgentString = WebViewConstants.WEBVIEW_USER_AGENT
            addJavascriptInterface(BannerJavaScriptInterface(), "monetai")

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    alpha = 1f
                }

                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.w(TAG, "Banner loading error: $errorCode - $description")
                }
            }
        }

        addView(webView)
    }
    
    private fun loadBanner() {
        val url = buildBannerUrl()
        webView.loadUrl(url)
    }
    
    private fun buildBannerUrl(): String {
        val bannerParams = bannerParams ?: return ""
        val baseUrl = "${WebViewConstants.WEB_BASE_URL}/banner"
        val style = bannerParams.style.value
        
        val queryParams = mutableListOf<String>()
        
        queryParams.add("discount=${bannerParams.discountPercent}")
        queryParams.add("locale=${bannerParams.locale}")
        
        // Use ISO8601 format with timezone info
        val endedAtIso = DateTimeHelper.formatToISO8601(bannerParams.endedAt)
        val encodedEndedAt = java.net.URLEncoder.encode(endedAtIso, "UTF-8")
        queryParams.add("endedAt=$encodedEndedAt")
        
        val queryString = queryParams.joinToString("&")
        return "$baseUrl/$style?$queryString"
    }
    
    // MARK: - Private Methods
    
    // MARK: - JavaScript Interface
    
    inner class BannerJavaScriptInterface {
        @JavascriptInterface
        fun postMessage(message: String) {
            if (message == WebViewConstants.MESSAGE_CLICK_BANNER) {
                onPaywallClickListener?.invoke()
            }
        }
    }
}
