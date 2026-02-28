package com.monetai.sdk

/**
 * Monetai SDK errors
 */
sealed class MonetaiError : Exception() {
    object NotInitialized : MonetaiError() {
        override val message: String = "SDK is not initialized"
    }
} 