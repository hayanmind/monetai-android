package com.monetai.sdk

/**
 * Monetai SDK errors
 */
sealed class MonetaiError : Exception() {
    object InvalidSDKKey : MonetaiError() {
        override val message: String = "Invalid SDK key"
    }
    
    object InvalidUserId : MonetaiError() {
        override val message: String = "Invalid user ID"
    }
    
    object NotInitialized : MonetaiError() {
        override val message: String = "SDK is not initialized"
    }
    
    object NetworkError : MonetaiError() {
        override val message: String = "Network error occurred"
    }
    
    object ServerError : MonetaiError() {
        override val message: String = "Server error occurred"
    }
    
    object BillingError : MonetaiError() {
        override val message: String = "Billing error occurred"
    }
} 