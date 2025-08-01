package com.monetai.sdk.models

/**
 * Prediction result
 */
enum class PredictResult(val value: String) {
    PURCHASER("purchaser"),
    NON_PURCHASER("non-purchaser");
    
    companion object {
        @JvmStatic
        fun fromString(value: String?): PredictResult? {
            return when (value) {
                "purchaser" -> PURCHASER
                "non-purchaser" -> NON_PURCHASER
                else -> null
            }
        }
    }
} 