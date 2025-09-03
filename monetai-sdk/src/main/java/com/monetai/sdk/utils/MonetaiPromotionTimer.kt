package com.monetai.sdk.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.monetai.sdk.models.DiscountInfo
import java.util.*

/**
 * Interface for handling promotion expiration events
 */
interface MonetaiPromotionTimerDelegate {
    fun promotionDidExpire()
}

/**
 * Manages promotion expiration timing with battery-optimized timer management
 * Follows iOS SDK implementation pattern for consistency
 */
class MonetaiPromotionTimer(
    private val context: Context,
    private val delegate: MonetaiPromotionTimerDelegate
) {
    
    companion object {
        private const val TAG = "MonetaiPromotionTimer"
        private const val CHECK_INTERVAL_MS = 1000L // 1 second like iOS
        
        // Intent actions for better readability
        private const val ACTION_SCREEN_OFF = Intent.ACTION_SCREEN_OFF
        private const val ACTION_SCREEN_ON = Intent.ACTION_SCREEN_ON
        private const val ACTION_USER_PRESENT = Intent.ACTION_USER_PRESENT
    }
    
    // MARK: - Properties
    private var discountInfo: DiscountInfo? = null
    private var handler: Handler? = null
    private var checkRunnable: Runnable? = null
    private var isTimerActive = false
    private var isAppInForeground = true
    
    // App lifecycle receiver for battery optimization
    private val appLifecycleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_SCREEN_OFF -> {
                    isAppInForeground = false
                    stopExpirationTimer()
                }
                ACTION_SCREEN_ON -> {
                    isAppInForeground = true
                    checkPromotionExpiration()
                    startExpirationTimerIfNeeded()
                }
                ACTION_USER_PRESENT -> {
                    checkPromotionExpiration()
                    startExpirationTimerIfNeeded()
                }
            }
        }
    }
    
    init {
        setupAppLifecycleObservers()
    }
    
    // MARK: - Public Methods
    
    /**
     * Configure the manager with discount information
     */
    fun configure(discountInfo: DiscountInfo) {
        this.discountInfo = discountInfo
        startExpirationTimerIfNeeded()
    }
    
    /**
     * Start monitoring promotion expiration
     */
    fun startMonitoring() {
        startExpirationTimerIfNeeded()
    }
    
    /**
     * Stop monitoring promotion expiration
     */
    fun stopMonitoring() {
        stopExpirationTimer()
    }
    
    /**
     * Check if promotion has expired (public method for immediate check)
     */
    fun checkExpiration() {
        checkPromotionExpiration()
    }
    
    /**
     * Cleanup resources
     */
    fun destroy() {
        stopExpirationTimer()
        try {
            context.unregisterReceiver(appLifecycleReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was already unregistered
        }
    }
    
    // MARK: - App Lifecycle Setup
    private fun setupAppLifecycleObservers() {
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_SCREEN_OFF)
            addAction(ACTION_SCREEN_ON)
            addAction(ACTION_USER_PRESENT)
        }
        
        try {
            context.registerReceiver(appLifecycleReceiver, intentFilter)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to register lifecycle observers", e)
        }
    }
    
    // MARK: - Promotion Expiration Management
    private fun checkPromotionExpiration() {
        val discountInfo = discountInfo ?: return
        
        val now = Date()
        val isExpired = now >= discountInfo.endedAt
        
        
        if (isExpired) {
            stopExpirationTimer()
            delegate.promotionDidExpire()
        }
    }
    
    private fun startExpirationTimerIfNeeded() {
        val discountInfo = discountInfo ?: return
        
        if (!isAppInForeground) {
            return
        }
        
        val timeUntilExpiration = discountInfo.endedAt.time - System.currentTimeMillis()
        
        if (timeUntilExpiration <= 0) {
            delegate.promotionDidExpire()
            return
        }
        
        startExpirationTimer()
    }
    
    private fun startExpirationTimer() {
        // Stop existing timer if running
        stopExpirationTimer()
        
        
        // Create handler on main thread (like iOS main queue)
        handler = Handler(Looper.getMainLooper())
        
        checkRunnable = object : Runnable {
            override fun run() {
                checkPromotionExpiration()
                
                // Schedule next check if timer is still active
                if (isTimerActive) {
                    handler?.postDelayed(this, CHECK_INTERVAL_MS)
                }
            }
        }
        
        handler?.postDelayed(checkRunnable!!, CHECK_INTERVAL_MS)
        isTimerActive = true
    }
    
    private fun stopExpirationTimer() {
        if (isTimerActive) {
            handler?.removeCallbacks(checkRunnable ?: return)
            handler = null
            checkRunnable = null
            isTimerActive = false
        }
    }
}