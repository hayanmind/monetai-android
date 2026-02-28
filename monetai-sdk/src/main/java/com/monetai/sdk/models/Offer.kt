package com.monetai.sdk.models

data class Offer(
    val agentId: Int,
    val agentName: String,
    val products: List<OfferProduct>
)

data class OfferProduct(
    val name: String,
    val sku: String,
    val discountRate: Double,
    val isManual: Boolean
)
