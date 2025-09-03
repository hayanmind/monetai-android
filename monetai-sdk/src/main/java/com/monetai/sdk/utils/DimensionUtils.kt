package com.monetai.sdk.utils

import android.content.Context

/**
 * Utility class for dimension conversions
 */
object DimensionUtils {
    
    /**
     * Converts dp to pixels
     * @param context Context for getting display metrics
     * @param dp Value in dp
     * @return Value in pixels
     */
    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
    
    /**
     * Converts dp to pixels
     * @param context Context for getting display metrics
     * @param dp Value in dp
     * @return Value in pixels
     */
    fun dpToPx(context: Context, dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}

/**
 * Extension functions for easy dp to px conversion
 */
fun Context.dpToPx(dp: Int): Int = DimensionUtils.dpToPx(this, dp)
fun Context.dpToPx(dp: Float): Int = DimensionUtils.dpToPx(this, dp)