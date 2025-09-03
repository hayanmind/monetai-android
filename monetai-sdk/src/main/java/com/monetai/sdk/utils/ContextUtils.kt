package com.monetai.sdk.utils

import android.app.Activity
import android.content.Context
import android.util.Log

/**
 * Utility class for Context operations
 */
object ContextUtils {
    
    private const val TAG = "ContextUtils"
    
    /**
     * Attempts to find the current Activity from the given Context
     * @param context The context to search from
     * @return Activity if found, null otherwise
     */
    fun findActivity(context: Context?): Activity? {
        return try {
            when (context) {
                is Activity -> {
                    Log.d(TAG, "Direct Activity context found")
                    context
                }
                else -> {
                    // Try to get from MonetaiSDK's stored context
                    val sdkContext = com.monetai.sdk.MonetaiSDK.shared.getApplicationContext()
                    if (sdkContext is Activity) {
                        Log.d(TAG, "Activity found from SDK context")
                        sdkContext
                    } else {
                        Log.w(TAG, "Context is not Activity, cannot find Activity")
                        Log.w(TAG, "Hint: Make sure to call SDK.initialize() from Activity context")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find Activity: ${e.message}")
            null
        }
    }
}