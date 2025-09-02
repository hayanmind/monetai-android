package com.monetai.sdk.utils

import android.os.Handler
import android.os.Looper

/**
 * Utility class for thread management operations
 */
object ThreadUtils {
    
    /**
     * Executes the given action on the main thread
     * If already on main thread, executes immediately
     * Otherwise, posts to main thread handler
     */
    fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            Handler(Looper.getMainLooper()).post(action)
        }
    }
    
    /**
     * Checks if current thread is the main thread
     */
    fun isMainThread(): Boolean {
        return Looper.myLooper() == Looper.getMainLooper()
    }
}