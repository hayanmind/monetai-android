package com.monetai.sdk.models

/**
 * Prediction response
 */
data class PredictResponse(
    val prediction: PredictResult?,  // Changed to nullable to match other platforms
    val testGroup: ABTestGroup?
) 