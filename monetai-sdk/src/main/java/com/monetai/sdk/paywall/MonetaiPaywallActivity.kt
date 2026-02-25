package com.monetai.sdk.paywall

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.monetai.sdk.constants.WebViewConstants
import com.monetai.sdk.models.PaywallParams
import com.monetai.sdk.models.PaywallStyle
import com.monetai.sdk.utils.DimensionUtils

/**
 * PaywallActivity displays the paywall using WebView
 * Provides the same functionality as iOS and React Native SDKs
 */
class MonetaiPaywallActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "MonetaiPaywallActivity"
        
        // Message constants (matching iOS/React Native)
        private const val MESSAGE_KEY_PURCHASE_BUTTON = "CLICK_PURCHASE_BUTTON"
        private const val MESSAGE_KEY_CLOSE_BUTTON = "CLICK_CLOSE_BUTTON"
        private const val MESSAGE_KEY_TERMS_OF_SERVICE = "CLICK_TERMS_OF_SERVICE"
        private const val MESSAGE_KEY_PRIVACY_POLICY = "CLICK_PRIVACY_POLICY"
        
        // Static callback holder for Activity instances using WeakReference to prevent memory leaks
        @Volatile
        private var staticOnClose: java.lang.ref.WeakReference<(() -> Unit)?>? = null
        @Volatile
        private var staticOnPurchase: java.lang.ref.WeakReference<((com.monetai.sdk.models.PaywallContext, (() -> Unit)) -> Unit)?>? = null
        @Volatile
        private var staticOnTermsOfService: java.lang.ref.WeakReference<((com.monetai.sdk.models.PaywallContext) -> Unit)?>? = null
        @Volatile
        private var staticOnPrivacyPolicy: java.lang.ref.WeakReference<((com.monetai.sdk.models.PaywallContext) -> Unit)?>? = null

        // Track the current running Activity instance for programmatic dismiss
        @Volatile
        private var currentInstance: java.lang.ref.WeakReference<MonetaiPaywallActivity>? = null

        internal fun setStaticCallbacks(
            onClose: (() -> Unit)?,
            onPurchase: ((com.monetai.sdk.models.PaywallContext, (() -> Unit)) -> Unit)?,
            onTermsOfService: ((com.monetai.sdk.models.PaywallContext) -> Unit)?,
            onPrivacyPolicy: ((com.monetai.sdk.models.PaywallContext) -> Unit)?
        ) {
            staticOnClose = java.lang.ref.WeakReference(onClose)
            staticOnPurchase = java.lang.ref.WeakReference(onPurchase)
            staticOnTermsOfService = java.lang.ref.WeakReference(onTermsOfService)
            staticOnPrivacyPolicy = java.lang.ref.WeakReference(onPrivacyPolicy)
        }

        internal fun getCurrentActivity(): MonetaiPaywallActivity? = currentInstance?.get()

        internal fun dismissCurrent() {
            currentInstance?.get()?.finish()
            currentInstance = null
        }
    }
    
    
    // MARK: - Properties
    private lateinit var paywallParams: PaywallParams
    private var onClose: (() -> Unit)? = null
    private var onPurchase: ((com.monetai.sdk.models.PaywallContext, (() -> Unit)) -> Unit)? = null
    private var onTermsOfService: ((com.monetai.sdk.models.PaywallContext) -> Unit)? = null
    private var onPrivacyPolicy: ((com.monetai.sdk.models.PaywallContext) -> Unit)? = null
    
    private lateinit var webView: WebView
    private lateinit var dimBackgroundView: View
    private lateinit var progressBar: ProgressBar
    private lateinit var rootView: FrameLayout
    
    // MARK: - Lifecycle
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get paywall params from intent
        paywallParams = intent.getParcelableExtra("paywallParams") 
            ?: throw IllegalArgumentException("PaywallParams is required")
        
        // Get static callbacks
        onClose = staticOnClose?.get()
        onPurchase = staticOnPurchase?.get()
        onTermsOfService = staticOnTermsOfService?.get()
        onPrivacyPolicy = staticOnPrivacyPolicy?.get()

        // Track this instance for programmatic dismiss
        currentInstance = java.lang.ref.WeakReference(this)
        
        setupUI()
        loadPaywall()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up static references to prevent memory leaks
        currentInstance = null
        staticOnClose = null
        staticOnPurchase = null
        staticOnTermsOfService = null
        staticOnPrivacyPolicy = null
        
        // Clean up WebView
        webView.destroy()
    }
    
    // MARK: - UI Setup
    
    private fun setupUI() {
        // Set main view background to clear
        rootView = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // PaywallActivity is a separate Activity, so no need for z-index/elevation controls
        
        // Setup dim background view (matching iOS 40% opacity)
        dimBackgroundView = View(this).apply {
            setBackgroundColor(0x66000000.toInt()) // 40% black (same as iOS)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Setup progress bar
        progressBar = ProgressBar(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
        }
        
        // Setup WebView
        setupWebView()
        
        // Add rounded corners for COMPACT style (matching iOS 12pt corner radius)
        if (paywallParams.style == PaywallStyle.COMPACT) {
            val cornerRadius = DimensionUtils.dpToPx(this@MonetaiPaywallActivity, 12).toFloat()
            webView.background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadii = floatArrayOf(
                    cornerRadius, cornerRadius, // top-left
                    cornerRadius, cornerRadius, // top-right
                    0f, 0f, // bottom-right
                    0f, 0f  // bottom-left
                )
                setColor(android.graphics.Color.TRANSPARENT)
            }
            webView.clipToOutline = true
        }
        
        // Hide WebView initially to prevent white flash
        webView.alpha = 0f
        
        // Add views to root
        rootView.addView(dimBackgroundView)
        rootView.addView(webView)
        rootView.addView(progressBar)
        
        setContentView(rootView)
        
        // Setup click listeners
        dimBackgroundView.setOnClickListener {
            onClose?.invoke()
            finish() // Immediately close the activity
        }
        
        // For COMPACT style, also allow root view clicks to close paywall
        if (paywallParams.style == PaywallStyle.COMPACT) {
            rootView.setOnClickListener {
                onClose?.invoke()
                finish()
            }
            
            // Prevent WebView clicks from propagating to parent
            webView.setOnClickListener {
                // Consume the click event to prevent it from reaching parent
            }
        }
        
        // Apply style-specific layout
        applyStyleSpecificLayout()
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        
        webView = WebView(this).apply {
            // WebView settings
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = false
                // Enable modern web features
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
            }
            
            // Set WebView background to transparent to prevent white flash
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            
            // Hide WebView initially to prevent white flash
            alpha = 0f
            
            // Set user agent
            settings.userAgentString = WebViewConstants.WEBVIEW_USER_AGENT
            
            // Add JavaScript interface
            addJavascriptInterface(PaywallJavaScriptInterface(), "monetai")
            
            // Set WebView client
            webViewClient = createWebViewClient()
        }
        
        // Apply style-specific WebView layout
        applyWebViewLayout()
    }
    
    private fun createWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                
                // Show WebView immediately after loading (no animation for snappy response)
                webView.alpha = 1f
            }
            
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e(TAG, "WebView error: $errorCode - $description")
                showError()
            }
            
            override fun onReceivedHttpError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                Log.e(TAG, "WebView HTTP error: ${errorResponse?.statusCode}")
                showError()
            }
        }
    }
    
    private fun applyStyleSpecificLayout() {
        when (paywallParams.style) {
            PaywallStyle.COMPACT -> {
                // Compact style: bottom sheet with dim background (matching iOS)
                dimBackgroundView.alpha = 1.0f
                webView.alpha = 0.0f
                
                // Convert 372dp to pixels for initial translation (matching iOS 372pt)
                webView.translationY = DimensionUtils.dpToPx(this@MonetaiPaywallActivity, 372).toFloat()
                
                // iOS-style slide-up animation: 0.3s duration with ease-out curve
                webView.post {
                    webView.animate()
                        .alpha(1.0f)
                        .translationY(0f)
                        .setDuration(300) // 0.3 seconds like iOS
                        .setInterpolator(AnimationUtils.loadInterpolator(this, android.R.interpolator.decelerate_quint)) // ease-out equivalent
                        .start()
                }
            }
            else -> {
                // Other styles: full screen coverage
                dimBackgroundView.alpha = 1.0f
                webView.alpha = 1.0f
                webView.translationY = 0f
            }
        }
    }
    
    private fun applyWebViewLayout() {
        when (paywallParams.style) {
            PaywallStyle.COMPACT -> {
                // Compact style: bottom-anchored with fixed height (matching iOS 372pt)
                webView.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    DimensionUtils.dpToPx(this@MonetaiPaywallActivity, 372)
                ).apply {
                    gravity = android.view.Gravity.BOTTOM
                    // No margins - full width like iOS
                }
            }
            else -> {
                // Other styles: full screen
                webView.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
        }
    }
    
    private fun loadPaywall() {
        val url = buildPaywallUrl()
        webView.loadUrl(url)
    }
    
    private fun buildPaywallUrl(): String {
        val baseUrl = "${WebViewConstants.WEB_BASE_URL}/paywall"
        val style = paywallParams.style.value
        
        val queryParams = mutableListOf<String>()
        
        if (paywallParams.discountPercent.isNotEmpty()) {
            queryParams.add("discount=${paywallParams.discountPercent}")
        }
        if (paywallParams.endedAt.isNotEmpty()) {
            val encodedEndedAt = java.net.URLEncoder.encode(paywallParams.endedAt, "UTF-8")
            queryParams.add("endedAt=$encodedEndedAt")
        }
        if (paywallParams.regularPrice.isNotEmpty()) {
            val encodedRegularPrice = java.net.URLEncoder.encode(paywallParams.regularPrice, "UTF-8")
            queryParams.add("regularPrice=$encodedRegularPrice")
        }
        if (paywallParams.discountedPrice.isNotEmpty()) {
            val encodedDiscountedPrice = java.net.URLEncoder.encode(paywallParams.discountedPrice, "UTF-8")
            queryParams.add("discountedPrice=$encodedDiscountedPrice")
        }
        if (paywallParams.locale.isNotEmpty()) {
            queryParams.add("locale=${paywallParams.locale}")
        }
        if (paywallParams.features.isNotEmpty()) {
            val featuresJson = paywallParams.features.joinToString(",") { feature ->
                "{\"title\":\"${feature.title}\",\"description\":\"${feature.description}\",\"isPremiumOnly\":${feature.isPremiumOnly}}"
            }
            val encodedFeatures = java.net.URLEncoder.encode("[$featuresJson]", "UTF-8")
            queryParams.add("features=$encodedFeatures")
        }
        
        val queryString = if (queryParams.isNotEmpty()) "?${queryParams.joinToString("&")}" else ""
        return "$baseUrl/$style$queryString"
    }
    
    private fun showError() {
        // Show error overlay similar to React Native
        val errorOverlay = FrameLayout(this).apply {
            setBackgroundColor(0xCC000000.toInt()) // 80% black
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        val errorCard = FrameLayout(this).apply {
            setBackgroundColor(0xFFFFFFFF.toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER
                setMargins(32, 32, 32, 32)
            }
        }
        
        // Add error card to error overlay
        errorOverlay.addView(errorCard)
        
        // Add error overlay to root
        rootView.addView(errorOverlay)
    }
    
    // MARK: - JavaScript Interface
    
    inner class PaywallJavaScriptInterface {
        @JavascriptInterface
        fun postMessage(message: String) {
            runOnUiThread {
                handleWebViewMessage(message)
            }
        }
    }
    
    private fun handleWebViewMessage(message: String) {
        
        when (message) {
            MESSAGE_KEY_PURCHASE_BUTTON -> {
                val purchaseCallback = onPurchase
                if (purchaseCallback != null) {
                    val paywallContext = com.monetai.sdk.models.PaywallContext(
                        activity = this@MonetaiPaywallActivity,
                        applicationContext = applicationContext
                    )
                    val closePaywall = { 
                        finish() 
                    }
                    purchaseCallback.invoke(paywallContext, closePaywall)
                } else {
                    Log.e(TAG, "❌ onPurchase callback is null!")
                }
            }
            MESSAGE_KEY_CLOSE_BUTTON -> {
                
                // First, directly close this Activity
                finish()
                
                // Then invoke callback for cleanup
                try {
                    onClose?.invoke()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error invoking close callback", e)
                }
            }
            MESSAGE_KEY_TERMS_OF_SERVICE -> {
                val paywallContext = com.monetai.sdk.models.PaywallContext(
                    activity = this@MonetaiPaywallActivity,
                    applicationContext = applicationContext
                )
                onTermsOfService?.invoke(paywallContext)
            }
            MESSAGE_KEY_PRIVACY_POLICY -> {
                val paywallContext = com.monetai.sdk.models.PaywallContext(
                    activity = this@MonetaiPaywallActivity,
                    applicationContext = applicationContext
                )
                onPrivacyPolicy?.invoke(paywallContext)
            }
            else -> {
                Log.w(TAG, "❓ Unknown message: '$message'")
            }
        }
    }
    
}
