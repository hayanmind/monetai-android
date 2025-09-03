package com.monetai.sdk.models

import java.util.Date

/**
 * Banner parameters for display
 */
data class BannerParams(
    val enabled: Boolean,                // Whether banner is enabled
    val isSubscriber: Boolean,           // Whether user is subscriber
    val locale: String,                  // Language setting
    val discountPercent: Int,            // Discount percentage
    val endedAt: Date,                   // Expiration date
    val style: PaywallStyle,             // Banner style
    val bottom: Float = 20f              // Bottom margin
)
