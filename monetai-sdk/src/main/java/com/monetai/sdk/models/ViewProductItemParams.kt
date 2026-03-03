package com.monetai.sdk.models

data class ViewProductItemParams(
    val productId: String,
    val price: Double,
    val regularPrice: Double,
    val currencyCode: String,
    val promotionId: Int? = null,
    val source: String? = null,
    val month: Int? = null
)
