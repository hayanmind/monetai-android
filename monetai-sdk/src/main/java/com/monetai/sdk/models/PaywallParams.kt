package com.monetai.sdk.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Paywall parameters for display
 * Provides the same functionality as iOS and React Native SDKs
 */
@Parcelize
data class PaywallParams(
    val discountPercent: String,        // Discount percentage (string)
    val endedAt: String,                // Expiration date (ISO8601)
    val regularPrice: String,           // Regular price
    val discountedPrice: String,        // Discounted price
    val locale: String,                 // Language setting
    val features: List<Feature>,        // Feature list
    val style: PaywallStyle            // Paywall style
) : Parcelable
