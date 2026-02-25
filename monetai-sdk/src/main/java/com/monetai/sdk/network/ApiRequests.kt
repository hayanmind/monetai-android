package com.monetai.sdk.network

import com.monetai.sdk.SDKVersion
import com.monetai.sdk.models.*
import com.monetai.sdk.utils.DateTimeHelper
import java.util.*
import android.util.Log

/**
 * API requests handler for Monetai SDK
 */
object ApiRequests {

    /**
     * Initialize SDK
     */
    suspend fun initialize(sdkKey: String, userId: String): InitializeResponse {
        return try {
            Log.d("ApiRequests", "Initializing SDK")

            val request = InitializeRequest(
                sdkKey = sdkKey,
                platform = "android",
                version = SDKVersion.getVersion()
            )

            val response = ApiClient.apiService.initialize(request)

            Log.d("ApiRequests", "SDK initialization successful")
            response
        } catch (e: Exception) {
            Log.e("ApiRequests", "SDK initialization failed", e)
            Log.e("ApiRequests", "Error type: ${e.javaClass.simpleName}")
            Log.e("ApiRequests", "Error message: ${e.message}")
            if (e is retrofit2.HttpException) {
                Log.e("ApiRequests", "HTTP Error Code: ${e.code()}")
                try {
                    Log.e("ApiRequests", "HTTP Error Response: ${e.response()?.errorBody()?.string()}")
                } catch (readError: Exception) {
                    Log.e("ApiRequests", "Failed to read error response body", readError)
                }
            }
            throw e
        }
    }

    /**
     * Create event
     */
    suspend fun createEvent(
        sdkKey: String,
        userId: String,
        eventName: String,
        params: Map<String, Any>? = null,
        createdAt: String
    ) {
        val request = CreateEventRequest(
            sdkKey = sdkKey,
            userId = userId,
            eventName = eventName,
            params = params,
            createdAt = createdAt,
            platform = "android"
        )

        ApiClient.apiService.createEvent(request)
    }

    /**
     * Get offer for a promotion
     */
    suspend fun getOffer(sdkKey: String, userId: String, promotionId: Int): Offer? {
        return try {
            val request = GetOfferRequest(
                sdkKey = sdkKey,
                userId = userId,
                promotionId = promotionId,
                platform = "android"
            )

            val response = ApiClient.apiService.getOffer(request)

            if (!response.isSuccessful) {
                throw retrofit2.HttpException(response)
            }

            response.body()?.let { body ->
                Offer(
                    agentId = body.agentId,
                    agentName = body.agentName,
                    products = body.products.map { product ->
                        OfferProduct(
                            name = product.name,
                            sku = product.sku,
                            discountRate = product.discountRate,
                            isManual = product.isManual
                        )
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("ApiRequests", "Failed to get offer", e)
            null
        }
    }

    /**
     * Log view product item event
     */
    suspend fun logViewProductItem(
        sdkKey: String,
        userId: String,
        params: ViewProductItemParams,
        createdAt: String
    ) {
        val request = ViewProductItemRequest(
            sdkKey = sdkKey,
            userId = userId,
            productId = params.productId,
            price = params.price,
            regularPrice = params.regularPrice,
            currencyCode = params.currencyCode,
            promotionId = params.promotionId,
            month = params.month,
            createdAt = createdAt,
            platform = "android"
        )

        ApiClient.apiService.logViewProductItem(request)
    }

    /**
     * Map transaction to user
     */
    suspend fun mapTransactionToUser(
        purchaseToken: String,
        packageName: String,
        sdkKey: String,
        userId: String
    ) {
        val request = TransactionMappingRequest(
            purchaseToken = purchaseToken,
            packageName = packageName,
            userId = userId,
            sdkKey = sdkKey
        )

        ApiClient.apiService.mapTransactionToUser(request)
    }

    /**
     * Send purchase history to server
     */
    suspend fun sendPurchaseHistory(
        purchases: List<PurchaseItem>,
        packageName: String,
        sdkKey: String,
        userId: String
    ) {
        val request = PurchaseHistoryRequest(
            packageName = packageName,
            userId = userId,
            sdkKey = sdkKey,
            purchases = purchases
        )

        ApiClient.apiService.sendPurchaseHistory(request)
    }
}
