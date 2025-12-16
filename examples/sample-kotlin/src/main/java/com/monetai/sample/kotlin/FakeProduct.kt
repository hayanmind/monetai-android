package com.monetai.sample.kotlin

data class FakeProduct(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val regularPrice: Double,
    val currencyCode: String,
    val month: Int?
)

