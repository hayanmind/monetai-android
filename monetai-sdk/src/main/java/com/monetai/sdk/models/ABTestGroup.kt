package com.monetai.sdk.models

/**
 * A/B test group
 */
enum class ABTestGroup(val value: String) {
    MONETAI("monetai"),
    BASELINE("baseline"),
    UNKNOWN("unknown");
    
    companion object {
        @JvmStatic
        fun fromString(value: String?): ABTestGroup? {
            return when (value) {
                "monetai" -> MONETAI
                "baseline" -> BASELINE
                "unknown" -> UNKNOWN
                else -> null
            }
        }
    }
} 