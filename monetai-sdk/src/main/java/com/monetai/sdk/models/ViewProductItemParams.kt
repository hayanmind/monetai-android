package com.monetai.sdk.models

data class ViewProductItemParams(
    val productId: String,
    val price: Double,
    val regularPrice: Double,
    val currencyCode: String,
    val placement: String,
    val month: Int? = null
)
