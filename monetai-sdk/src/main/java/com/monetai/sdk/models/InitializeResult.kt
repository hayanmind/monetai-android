package com.monetai.sdk.models

/**
 * SDK initialization result
 */
data class InitializeResult(
    val organizationId: Int,
    val platform: String,
    val version: String,
    val userId: String
)
