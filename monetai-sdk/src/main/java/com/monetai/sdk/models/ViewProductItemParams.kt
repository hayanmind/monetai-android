package com.monetai.sdk.models

data class ViewProductItemParams(
    val placement: String,
    val productId: String,
    val price: Double,
    val regularPrice: Double,
    val currencyCode: String,
    val month: Int? = null
)
