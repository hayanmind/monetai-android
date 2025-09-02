package com.monetai.sdk.models

import java.util.Date

/**
 * Discount information
 */
data class DiscountInfo(
    val startedAt: Date,                // Start date
    val endedAt: Date,                   // End date
    val userId: String,                  // User ID
    val sdkKey: String                   // SDK key
)
