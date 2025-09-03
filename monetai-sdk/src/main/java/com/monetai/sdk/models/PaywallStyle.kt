package com.monetai.sdk.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Paywall display style types
 */
@Parcelize
enum class PaywallStyle(val value: String) : Parcelable {
    COMPACT("compact"),                     // Compact style
    HIGHLIGHT_BENEFITS("highlight-benefits"), // Highlight benefits style
    KEY_FEATURE_SUMMARY("key-feature-summary"), // Key feature summary style
    TEXT_FOCUSED("text-focused")           // Text focused style
}
