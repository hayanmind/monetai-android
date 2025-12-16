package com.monetai.sdk.network

import com.monetai.sdk.MonetaiError
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
    suspend fun initialize(sdkKey: String, userId: String): Pair<InitializeResponse, com.monetai.sdk.models.ABTestResponse> {
        return try {
            Log.d("ApiRequests", "Initializing SDK")
            
            val request = InitializeRequest(
                sdkKey = sdkKey,
                platform = "android",
                version = SDKVersion.getVersion()
            )
            
            val response = ApiClient.apiService.initialize(request)
            
            // Get AB test group from actual API
            val abTestRequest = ABTestRequest(
                sdkKey = sdkKey,
                userId = userId,
                platform = "android"
            )
            
            val abTestResponse = ApiClient.apiService.getABTestGroup(abTestRequest)
            
            Log.d("ApiRequests", "SDK initialization successful")
            Pair(response, abTestResponse.toABTestResponse())
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
            // Preserve original exception for better debugging
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
        createdAt: Date = Date()
    ) {
        try {
            // Use DateTimeHelper for ISO 8601 formatting
            val formattedDate = DateTimeHelper.formatToISO8601(createdAt)
            
            val request = CreateEventRequest(
                sdkKey = sdkKey,
                userId = userId,
                eventName = eventName,
                params = params,
                createdAt = formattedDate,
                platform = "android"
            )
            
            val response: EmptyResponse = ApiClient.apiService.createEvent(request)
            // EmptyResponse is expected for successful event creation
        } catch (e: Exception) {
            // Preserve original exception for better debugging
            throw e
        }
    }
    
    /**
     * Log product item view event
     */
    suspend fun logViewProductItem(
        sdkKey: String,
        userId: String,
        params: ViewProductItemParams,
        createdAt: Date = Date()
    ) {
        try {
            val formattedDate = DateTimeHelper.formatToISO8601(createdAt)
            
            val request = ViewProductItemRequest(
                sdkKey = sdkKey,
                userId = userId,
                productId = params.productId,
                price = params.price,
                regularPrice = params.regularPrice,
                currencyCode = params.currencyCode,
                month = params.month,
                createdAt = formattedDate,
                platform = "android"
            )
            
            ApiClient.apiService.logViewProductItem(request)
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * Predict user behavior
     */
    suspend fun predict(sdkKey: String, userId: String): PredictApiResponse {
        return try {
            val request = PredictRequest(
                sdkKey = sdkKey,
                userId = userId
            )
            
            ApiClient.apiService.predict(request)
        } catch (e: Exception) {
            // Preserve original exception for better debugging
            throw e
        }
    }
    
    /**
     * Get app user discount
     */
    suspend fun getAppUserDiscount(sdkKey: String, userId: String): AppUserDiscount? {
        return try {
            val response = ApiClient.apiService.getAppUserDiscount(sdkKey, userId)
            response.discount?.toAppUserDiscount()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Create app user discount
     */
    suspend fun createAppUserDiscount(
        sdkKey: String,
        userId: String,
        startedAt: Date,
        endedAt: Date
    ): AppUserDiscount {
        return try {
            // Use DateTimeHelper for ISO 8601 formatting
            val request = CreateDiscountRequest(
                sdkKey = sdkKey,
                appUserId = userId,
                startedAt = DateTimeHelper.formatToISO8601(startedAt),
                endedAt = DateTimeHelper.formatToISO8601(endedAt)
            )
            
            val response = ApiClient.apiService.createAppUserDiscount(request)
            response.discount.toAppUserDiscount()
        } catch (e: Exception) {
            // Preserve original exception for better debugging
            throw e
        }
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
        try {
            val request = TransactionMappingRequest(
                purchaseToken = purchaseToken,
                packageName = packageName,
                userId = userId,
                sdkKey = sdkKey
            )
            
            ApiClient.apiService.mapTransactionToUser(request)
        } catch (e: Exception) {
            // Preserve original exception for better debugging
            throw e
        }
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
        try {
            val request = PurchaseHistoryRequest(
                packageName = packageName,
                userId = userId,
                sdkKey = sdkKey,
                purchases = purchases
            )
            
            ApiClient.apiService.sendPurchaseHistory(request)
        } catch (e: Exception) {
            // Preserve original exception for better debugging
            throw e
        }
    }
} 