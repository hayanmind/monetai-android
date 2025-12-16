package com.monetai.sdk.models

/**
 * Parameters for logging a product item view event.
 */
data class ViewProductItemParams(
    val productId: String,
    val price: Double,
    val regularPrice: Double,
    val currencyCode: String,
    val month: Int?
)

