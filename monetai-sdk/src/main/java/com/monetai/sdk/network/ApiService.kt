package com.monetai.sdk.network

import retrofit2.Response
import retrofit2.http.*

/**
 * API service interface for Monetai SDK
 */
interface ApiService {

    @POST("sdk-integrations")
    suspend fun initialize(
        @Body request: InitializeRequest
    ): InitializeResponse

    @POST("events")
    suspend fun createEvent(
        @Body request: CreateEventRequest
    ): EmptyResponse

    @POST("offers/get-offer")
    suspend fun getOffer(
        @Body request: GetOfferRequest
    ): Response<GetOfferResponse>

    @POST("events/view-product-item")
    suspend fun logViewProductItem(
        @Body request: ViewProductItemRequest
    ): EmptyResponse

    // Actual API endpoints
    @POST("transaction-id-to-user-id/android")
    suspend fun mapTransactionToUser(
        @Body request: TransactionMappingRequest
    ): EmptyResponse

    @POST("transaction-id-to-user-id/android/receipt")
    suspend fun sendPurchaseHistory(
        @Body request: PurchaseHistoryRequest
    ): EmptyResponse
}

// Request/Response models
data class InitializeRequest(
    val sdkKey: String,
    val platform: String = "android",
    val version: String
)

// Matches actual API server response format
data class InitializeResponse(
    val organization_id: Int,
    val platform: String,
    val version: String,
    val server_timestamp: Long
)

data class CreateEventRequest(
    val sdkKey: String,
    val userId: String,
    val eventName: String,
    val params: Map<String, Any>?,
    val createdAt: String,
    val platform: String = "android"
)

// Offer API request/response models
data class GetOfferRequest(
    val sdkKey: String,
    val userId: String,
    val promotionId: Int,
    val platform: String = "android"
)

data class GetOfferResponse(
    val agentId: Int,
    val agentName: String,
    val products: List<OfferProductResponse>
)

data class OfferProductResponse(
    val name: String,
    val sku: String,
    val discountRate: Double,
    val isManual: Boolean
)

// View Product Item API request model
data class ViewProductItemRequest(
    val sdkKey: String,
    val userId: String,
    val productId: String,
    val price: Double,
    val regularPrice: Double,
    val currencyCode: String,
    val promotionId: Int,
    val month: Int?,
    val createdAt: String,
    val platform: String = "android"
)

// Actual data structures
data class TransactionMappingRequest(
    val purchaseToken: String,
    val packageName: String,
    val userId: String,
    val sdkKey: String
)

data class PurchaseHistoryRequest(
    val packageName: String,
    val userId: String,
    val sdkKey: String,
    val purchases: List<PurchaseItem>
)

data class PurchaseItem(
    val purchaseToken: String
)

class EmptyResponse()
