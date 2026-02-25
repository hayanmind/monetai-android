package com.monetai.sdk.models

import android.app.Activity
import android.content.Context

/**
 * Context information provided to paywall callbacks
 */
data class PaywallContext(
    val activity: Activity,              // Current activity for UI operations
    val applicationContext: Context      // Application context for resources
)

/**
 * Paywall configuration with Context Injection Pattern
 * Provides the same functionality as iOS and React Native SDKs
 */
data class PaywallConfig @JvmOverloads constructor(
    val discountPercent: Int,           // Discount percentage (0-100)
    val regularPrice: String,           // Regular price
    val discountedPrice: String,        // Discounted price
    val locale: String,                 // Language setting
    val style: PaywallStyle,            // Paywall style
    val features: List<Feature> = emptyList(), // Feature list
    val bannerBottom: Float = 20f,     // Banner bottom margin

    // Context Injection Pattern - callbacks receive context for UI operations
    val onPurchase: ((PaywallContext, (() -> Unit)) -> Unit)? = null,      // Purchase handling with context
    val onTermsOfService: ((PaywallContext) -> Unit)? = null,              // Terms of service with context
    val onPrivacyPolicy: ((PaywallContext) -> Unit)? = null               // Privacy policy with context
)
