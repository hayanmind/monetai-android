package com.monetai.sdk.constants

/**
 * Common WebView constants shared between Banner and Paywall
 * Matches iOS SDK configuration
 */
object WebViewConstants {
    /// Base URL for monetai web paywall/banner (matching iOS SDK)
    const val WEB_BASE_URL = "https://dashboard.monetai.io/webview"
    
    /// User-Agent for SDK webviews (matching iOS SDK)
    const val WEBVIEW_USER_AGENT = "MonetaiSDK"
    
    /// Message constants for JavaScript communication
    const val MESSAGE_CLICK_BANNER = "CLICK_BANNER"
}
