package com.monetai.sdk.models

/**
 * Event logging options
 */
data class LogEventOptions(
    val eventName: String,
    val params: Map<String, Any>? = null
) {
    companion object {
        /**
         * Basic event (without parameters)
         */
        @JvmStatic
        fun event(eventName: String): LogEventOptions {
            return LogEventOptions(eventName = eventName)
        }
        
        /**
         * Event with parameters
         */
        @JvmStatic
        fun event(eventName: String, params: Map<String, Any>): LogEventOptions {
            return LogEventOptions(eventName = eventName, params = params)
        }
    }
} 