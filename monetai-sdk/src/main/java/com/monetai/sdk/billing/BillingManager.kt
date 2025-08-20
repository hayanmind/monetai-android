package com.monetai.sdk.billing

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.monetai.sdk.MonetaiSDK
import com.monetai.sdk.network.ApiRequests
import com.monetai.sdk.network.PurchaseItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

// MARK: - Receipt Validator
/**
 * Receipt validation and transmission logic
 */
class ReceiptValidator(
    private val context: Context,
    private val sdkKey: String,
    private val userId: String
) {
    companion object {
        private const val TAG = "ReceiptValidator"
        private const val RECEIPT_VALIDATION_URL = "https://monetai-api-414410537412.us-central1.run.app/sdk/transaction-id-to-user-id/android/receipt"
    }
    
    /**
     * Send receipt
     */
    suspend fun sendReceipt() {
        Log.d(TAG, "[Debug] sendReceipt started")
        
        val packageName = context.packageName
        
        Log.d(TAG, "[Debug] Credentials check completed")
        
        Log.d(TAG, "[Debug] calling queryAndSyncPurchaseHistory")
        queryAndSyncPurchaseHistory(userId, packageName, sdkKey)
    }
    
    /**
     * Query and sync purchase history
     */
    private suspend fun queryAndSyncPurchaseHistory(userId: String, packageName: String, sdkKey: String) {
        Log.d(TAG, "[Debug] queryAndSyncPurchaseHistory started")
        
        val deferred = CompletableDeferred<Unit>()
        
        val billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, _ ->
                // Dummy listener for purchase history query
                Log.d(TAG, "[Debug] dummy listener called: ${billingResult.responseCode}")
            }
            .enablePendingPurchases()
            .build()
            
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "[Debug] onBillingSetupFinished: ${billingResult.responseCode}")
                
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "[Debug] starting in-app purchase history query")
                    // Query in-app purchase history
                    val inappParams = QueryPurchaseHistoryParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                        
                    billingClient.queryPurchaseHistoryAsync(inappParams) { result1, inappPurchases ->
                        Log.d(TAG, "[Debug] in-app purchase history query completed: ${result1.responseCode}, count: ${inappPurchases?.size ?: 0}")
                        
                        // Query subscription history
                        Log.d(TAG, "[Debug] starting subscription history query")
                        val subsParams = QueryPurchaseHistoryParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                            
                        billingClient.queryPurchaseHistoryAsync(subsParams) { result2, subsPurchases ->
                            try {
                                Log.d(TAG, "[Debug] subscription history query completed: ${result2.responseCode}, count: ${subsPurchases?.size ?: 0}")
                                
                                val allPurchases = (inappPurchases ?: emptyList()) + (subsPurchases ?: emptyList())
                                
                                // Add debugging logs
                                Log.d(TAG, "[Debug] total purchase history count: ${allPurchases.size}")
                                Log.d(TAG, "[Debug] in-app purchase count: ${inappPurchases?.size ?: 0}")
                                Log.d(TAG, "[Debug] subscription purchase count: ${subsPurchases?.size ?: 0}")
                                
                                if (allPurchases.isNotEmpty()) {
                                    sendPurchaseHistoryToServer(userId, packageName, sdkKey, allPurchases)
                                    Log.d(TAG, "[Debug] purchase history server transmission completed")
                                } else {
                                    Log.d(TAG, "[Debug] no purchase history found")
                                }
                                
                                billingClient.endConnection()
                                deferred.complete(Unit)
                            } catch (e: Exception) {
                                Log.e(TAG, "[Error] error during purchase history processing: ${e.message}")
                                billingClient.endConnection()
                                deferred.completeExceptionally(e)
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "[Error] Billing setup failed: ${billingResult.debugMessage}")
                    billingClient.endConnection()
                    deferred.completeExceptionally(Exception("Billing setup failed: ${billingResult.debugMessage}"))
                }
            }
            
            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "[Debug] Billing service disconnected")
                if (!deferred.isCompleted) {
                    deferred.completeExceptionally(Exception("Billing service disconnected"))
                }
            }
        })
        
        // Wait for callback to complete
        deferred.await()
    }
    
    /**
     * Send purchase history to server
     */
    private fun sendPurchaseHistoryToServer(
        userId: String, 
        packageName: String, 
        sdkKey: String, 
        purchases: List<PurchaseHistoryRecord>
    ) {
        val purchasesList = purchases.map { purchase ->
            JSONObject().apply {
                put("purchaseToken", purchase.purchaseToken)
                // PurchaseHistoryRecord doesn't have orderId
            }
        }
        
        val payload = JSONObject().apply {
            put("packageName", packageName)
            put("userId", userId)
            put("sdkKey", sdkKey)
            put("purchases", JSONArray(purchasesList))
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(RECEIPT_VALIDATION_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                connection.outputStream.use { os ->
                    os.write(payload.toString().toByteArray())
                }
                
                val responseCode = connection.responseCode
                Log.d(TAG, "[Debug] purchase history sync response: $responseCode")
            } catch (e: Exception) {
                Log.e(TAG, "[Error] purchase history sync failed: ${e.message}")
            }
        }
    }
}

/**
 * Billing manager for handling in-app purchases and subscriptions
 * Updated for Google Play Billing Library 7.0.0
 */
class BillingManager(
    private val context: Context,
    private val sdkKey: String,
    private val userId: String
) : 
    PurchasesUpdatedListener, BillingClientStateListener {
    
    companion object {
        private const val TAG = "BillingManager"
        private const val MAPPING_URL = "https://monetai-api-414410537412.us-central1.run.app/sdk/transaction-id-to-user-id/android"
    }
    
    private var billingClient: BillingClient? = null

    /**
     * Start purchase observation
     */
    fun startObserving() {
        Log.d(TAG, "[Debug] BillingManager startObserving")
        
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
            
        billingClient?.startConnection(this)
    }
    
    /**
     * Stop purchase observation
     */
    fun stopObserving() {
        billingClient?.endConnection()
        billingClient = null
    }
    
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Log.d(TAG, "[Debug] onBillingSetupFinished: ${billingResult.responseCode}")
        
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.d(TAG, "[Debug] Billing client ready - waiting for new purchases")
        } else {
            Log.e(TAG, "[Error] Billing setup failed: ${billingResult.debugMessage}")
        }
    }
    
    override fun onBillingServiceDisconnected() {
        Log.d(TAG, "[Debug] onBillingServiceDisconnected")
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        Log.d(TAG, "[Debug] onPurchasesUpdated: ${billingResult.responseCode}, count: ${purchases?.size ?: 0}")
        
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else {
            Log.e(TAG, "[Error] Purchase update failed: ${billingResult.debugMessage}")
        }
    }
    
    /**
     * Handle purchase
     */
    private fun handlePurchase(purchase: Purchase) {
        val purchaseToken = purchase.purchaseToken
        
        Log.d(TAG, "[Debug] handlePurchase: purchaseToken=$purchaseToken, state=${purchase.purchaseState}")
        
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            sendMapping(purchaseToken)
        }
    }
    
    /**
     * Send purchase mapping
     */
    private fun sendMapping(purchaseToken: String) {
        Log.d(TAG, "[Debug] sendMapping: $purchaseToken")
        
        val packageName = context.packageName
        
        val payload = JSONObject().apply {
            put("purchaseToken", purchaseToken)
            put("packageName", packageName)
            put("userId", userId)
            put("sdkKey", sdkKey)
        }
        
        Log.d(TAG, "[Debug] Sending purchase mapping to server")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(MAPPING_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                
                connection.outputStream.use { os ->
                    os.write(payload.toString().toByteArray())
                }
                
                val responseCode = connection.responseCode
                Log.d(TAG, "[Debug] Mapping POST succeeded: $responseCode")
            } catch (e: Exception) {
                Log.e(TAG, "[Error] Mapping POST failed: ${e.message}")
            }
        }
    }
} 