package com.monetai.sdk.billing

import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.monetai.sdk.network.ApiRequests
import com.monetai.sdk.network.PurchaseItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred

// MARK: - Receipt Validator
/**
 * Receipt validation and transmission logic
 */
class ReceiptValidator(
    private val context: Context,
    private val sdkKey: String,
    private val userId: String,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "ReceiptValidator"
    }

    /**
     * Send receipt
     */
    suspend fun sendReceipt() {
        val packageName = context.packageName
        queryAndSyncActivePurchases(userId, packageName, sdkKey)
    }

    /**
     * Query and sync active purchases
     */
    private suspend fun queryAndSyncActivePurchases(userId: String, packageName: String, sdkKey: String) {
        val deferred = CompletableDeferred<Unit>()

        val billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, _ ->
                Log.d(TAG, "[Debug] dummy listener called: ${billingResult.responseCode}")
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    billingClient.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    ) { _, inappPurchases ->
                        billingClient.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        ) { _, subsPurchases ->
                            val allPurchases = inappPurchases + subsPurchases

                            if (allPurchases.isNotEmpty()) {
                                scope.launch {
                                    try {
                                        val purchaseItems = allPurchases.map { PurchaseItem(purchaseToken = it.purchaseToken) }
                                        ApiRequests.sendPurchaseHistory(
                                            purchases = purchaseItems,
                                            packageName = packageName,
                                            sdkKey = sdkKey,
                                            userId = userId
                                        )
                                        Log.d(TAG, "[Debug] active purchases server transmission completed")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "[Error] active purchases sync failed: ${e.message}")
                                    } finally {
                                        billingClient.endConnection()
                                        deferred.complete(Unit)
                                    }
                                }
                            } else {
                                Log.d(TAG, "[Debug] no active purchases found")
                                billingClient.endConnection()
                                deferred.complete(Unit)
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

        deferred.await()
    }
}

/**
 * Billing manager for handling in-app purchases and subscriptions
 */
class BillingManager(
    private val context: Context,
    private val sdkKey: String,
    private val userId: String,
    private val scope: CoroutineScope
) :
    PurchasesUpdatedListener, BillingClientStateListener {

    companion object {
        private const val TAG = "BillingManager"
    }

    private var billingClient: BillingClient? = null

    /**
     * Start purchase observation
     */
    fun startObserving() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
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
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            sendMapping(purchase.purchaseToken)
        }
    }

    /**
     * Send purchase mapping
     */
    private fun sendMapping(purchaseToken: String) {
        val packageName = context.packageName

        scope.launch {
            try {
                ApiRequests.mapTransactionToUser(
                    purchaseToken = purchaseToken,
                    packageName = packageName,
                    sdkKey = sdkKey,
                    userId = userId
                )
                Log.d(TAG, "[Debug] Mapping POST succeeded")
            } catch (e: Exception) {
                Log.e(TAG, "[Error] Mapping POST failed: ${e.message}")
            }
        }
    }
}
