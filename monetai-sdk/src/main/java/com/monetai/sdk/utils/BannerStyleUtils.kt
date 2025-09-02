package com.monetai.sdk.utils

import com.monetai.sdk.models.PaywallStyle

/**
 * Utility class for banner style calculations
 * Consolidates style-related logic that was duplicated between BannerManager and BannerView
 */
object BannerStyleUtils {
    
    /**
     * Get banner height based on style (matching iOS implementation exactly)
     */
    fun getBannerHeight(style: PaywallStyle): Int {
        return when (style) {
            PaywallStyle.TEXT_FOCUSED -> 45          // iOS: 45
            PaywallStyle.COMPACT -> 68               // iOS: 68
            PaywallStyle.KEY_FEATURE_SUMMARY -> 56   // iOS: 56 (default)
            PaywallStyle.HIGHLIGHT_BENEFITS -> 56    // iOS: 56 (default)
            else -> 56                               // iOS: 56 (default)
        }
    }
    
    /**
     * Get banner corner radius based on style (matching iOS implementation exactly)
     */
    fun getBannerCornerRadius(style: PaywallStyle): Int {
        return when (style) {
            PaywallStyle.TEXT_FOCUSED -> 12          // iOS: 12
            PaywallStyle.COMPACT -> 16               // iOS: 16
            PaywallStyle.KEY_FEATURE_SUMMARY -> 16   // iOS: 16
            PaywallStyle.HIGHLIGHT_BENEFITS -> 12    // iOS: 12
            else -> 12                               // iOS: 12 (default)
        }
    }
    
    /**
     * Get banner horizontal margin based on style (matching iOS implementation exactly)
     */
    fun getBannerHorizontalMargin(style: PaywallStyle): Int {
        return when (style) {
            PaywallStyle.TEXT_FOCUSED -> 16          // iOS: 16
            PaywallStyle.COMPACT -> 16               // iOS: 16
            PaywallStyle.KEY_FEATURE_SUMMARY -> 16   // iOS: 16
            PaywallStyle.HIGHLIGHT_BENEFITS -> 16    // iOS: 16
            else -> 16                               // iOS: 16 (default)
        }
    }
}