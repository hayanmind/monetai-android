package com.monetai.sdk.models

import java.util.Date

/**
 * Discount information for banner/paywall display timing
 */
data class DiscountInfo(
    val startedAt: Date,
    val endedAt: Date,
    val userId: String,
    val sdkKey: String
)
