package com.monetai.sdk.banner

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
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
import java.util.*

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
        Log.d(TAG, "👁️ MonetaiBannerView.show() called")
        Log.d(TAG, "  Current visibility: $visibility")
        Log.d(TAG, "  Current width: $width, height: $height")
        Log.d(TAG, "  Current x: $x, y: $y")
        Log.d(TAG, "  Parent: $parent")
        Log.d(TAG, "  LayoutParams: $layoutParams")
        
        visibility = View.VISIBLE
        
        // Force layout update
        requestLayout()
        invalidate()
        
        Log.d(TAG, "✅ Visibility set to VISIBLE")
        Log.d(TAG, "  New visibility: $visibility")
        Log.d(TAG, "  New width: $width, height: $height")
        Log.d(TAG, "  New x: $x, y: $y")
        
        // Post a delayed check to see if dimensions are set
        post {
            Log.d(TAG, "🔄 Post-check dimensions:")
            Log.d(TAG, "  Width: $width, Height: $height")
            Log.d(TAG, "  X: $x, Y: $y")
            Log.d(TAG, "  Is visible: ${visibility == View.VISIBLE}")
            Log.d(TAG, "  Alpha: $alpha")
        }
    }
    
    fun hide() {
        visibility = View.GONE
    }
    
    // MARK: - Private Methods
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        Log.d(TAG, "🔧 setupWebView() called")
        
        // Set banner dimensions based on style
        val style = bannerParams?.style ?: PaywallStyle.KEY_FEATURE_SUMMARY
        val bannerHeightDp = BannerStyleUtils.getBannerHeight(style)
        val heightPx = DimensionUtils.dpToPx(context, bannerHeightDp)
        
        Log.d(TAG, "    Banner height: ${bannerHeightDp}dp = ${heightPx}px")
        
        // Set banner view size using ViewGroup.LayoutParams
        var newLayoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            heightPx
        )
        
        Log.d(TAG, "    Created LayoutParams: width=${newLayoutParams.width}, height=${newLayoutParams.height}")
        
        // Set banner position (bottom)
        val bannerParams = bannerParams
        if (bannerParams != null) {
            val bottomMarginPx = DimensionUtils.dpToPx(context, bannerParams.bottom)
            Log.d(TAG, "    Bottom margin: ${bannerParams.bottom}dp = ${bottomMarginPx}px")
            
            // Create MarginLayoutParams directly to avoid smart cast issues
            val marginLayoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heightPx
            )
            marginLayoutParams.bottomMargin = bottomMarginPx
            newLayoutParams = marginLayoutParams
            
            Log.d(TAG, "    ✅ Final LayoutParams: width=${marginLayoutParams.width}, height=${marginLayoutParams.height}, bottomMargin=${marginLayoutParams.bottomMargin}")
        }
        
        // Apply layout params
        layoutParams = newLayoutParams
        Log.d(TAG, "    Applied layoutParams: $layoutParams")
        
        // Apply corner radius based on style (matching iOS implementation)
        val cornerRadiusDp = BannerStyleUtils.getBannerCornerRadius(style)
        val cornerRadiusPx = DimensionUtils.dpToPx(context, cornerRadiusDp)
        Log.d(TAG, "    Corner radius: ${cornerRadiusDp}dp = ${cornerRadiusPx}px")
        
        // Set corner radius for the banner view
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.TRANSPARENT)
            setCornerRadius(cornerRadiusPx.toFloat())
        }
        clipToOutline = true
        Log.d(TAG, "    ✅ Corner radius applied: ${cornerRadiusDp}dp")
        
        Log.d(TAG, "    Creating WebView...")
        webView = WebView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
            
            // Hide WebView initially to prevent flickering
            alpha = 0f
            
            Log.d(TAG, "    WebView LayoutParams: width=${layoutParams.width}, height=${layoutParams.height}")
            
            // WebView settings
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = false
                // Enable hardware acceleration for smoother rendering
                setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
            }
            
            // Set layer type for better performance
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // Set user agent
            settings.userAgentString = WebViewConstants.WEBVIEW_USER_AGENT
            
            // Add JavaScript interface
            addJavascriptInterface(BannerJavaScriptInterface(), "monetai")
            
            // Set WebView client
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "✅ Banner loaded successfully: $url")
                    
                    // Show WebView immediately after loading
                    alpha = 1f
                }
                
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.w(TAG, "❌ Banner loading error: $errorCode - $description")
                }
            }
        }
        
        Log.d(TAG, "    Adding WebView to MonetaiBannerView...")
        addView(webView)
        Log.d(TAG, "    ✅ WebView added. Child count: $childCount")
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
            Log.d(TAG, "📞 JavaScript message received: '$message'")
            if (message == WebViewConstants.MESSAGE_CLICK_BANNER) {
                Log.d(TAG, "🎯 Banner click detected - invoking paywall listener")
                onPaywallClickListener?.invoke()
            } else {
                Log.d(TAG, "❓ Unknown message: '$message'")
            }
        }
    }
}
