package com.monetai.sdk.models

/**
 * Campaign information
 */
data class Campaign(
    val id: Int,
    val name: String,
    val exposureTimeSec: Int,
    val discountRate: Double,
    val discountType: String
) 