package com.monetai.sdk.network

import com.monetai.sdk.models.*
import com.monetai.sdk.utils.DateTimeHelper
import retrofit2.http.*
import java.util.Date

/**
 * API service interface for Monetai SDK
 */
interface ApiService {
    
    @POST("sdk-integrations")
    suspend fun initialize(
        @Body request: InitializeRequest
    ): InitializeResponse
    
    @POST("ab-test")
    suspend fun getABTestGroup(
        @Body request: ABTestRequest
    ): ABTestResponse
    
    @POST("events")
    suspend fun createEvent(
        @Body request: CreateEventRequest
    ): EmptyResponse
    
    @POST("predict")
    suspend fun predict(
        @Body request: PredictRequest
    ): PredictApiResponse
    
    @GET("app-user-discounts/latest")
    suspend fun getAppUserDiscount(
        @Query("sdkKey") sdkKey: String,
        @Query("appUserId") appUserId: String
    ): GetDiscountResponse
    
    @POST("app-user-discounts")
    suspend fun createAppUserDiscount(
        @Body request: CreateDiscountRequest
    ): CreateDiscountResponse
    
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

// AB Test API request/response models
data class ABTestRequest(
    val sdkKey: String,
    val userId: String,
    val platform: String = "android"
)

// Matches actual API server response format
data class ABTestResponse(
    val group: String?,  // 'baseline' | 'monetai' | 'unknown' | null
    val campaign: CampaignResponse?
) {
    fun toABTestResponse(): com.monetai.sdk.models.ABTestResponse {
        return com.monetai.sdk.models.ABTestResponse(
            group = ABTestGroup.fromString(group),
            campaign = campaign?.toCampaign()
        )
    }
}

// Matches actual API server response format (snake_case)
data class CampaignResponse(
    val id: Int,
    val created_at: String?,  // ISO 8601 format string
    val organization_id: Int,
    val campaign_name: String,
    val started_at: String?,  // ISO 8601 format string
    val ended_at: String?,    // ISO 8601 format string
    val traffic_ratio: Double,
    val allocation_ratio: Double,
    val discount_ratio: Double,
    val exposure_time_sec: Int,
    val model_accuracy: Double?
) {
    fun toCampaign(): Campaign {
        return Campaign(
            id = id,
            name = campaign_name,
            exposureTimeSec = exposure_time_sec,
            discountRate = discount_ratio,
            discountType = "percentage" // default value
        )
    }
}

// Matches actual API server response format
data class InitializeResponse(
    val organization_id: Int,  // changed to snake_case
    val platform: String,
    val version: String
)

data class CreateEventRequest(
    val sdkKey: String,
    val userId: String,
    val eventName: String,
    val params: Map<String, Any>?,
    val createdAt: String,
    val platform: String = "android"
)

data class PredictRequest(
    val sdkKey: String,
    val userId: String
)

// Matches actual API server response format
data class PredictApiResponse(
    val prediction: String?,  // can be null
    val testGroup: String?    // can be null
) {
    fun toPredictResponse(): PredictResponse {
        return PredictResponse(
            prediction = PredictResult.fromString(prediction),  // Allow null values to pass through
            testGroup = ABTestGroup.fromString(testGroup)
        )
    }
}

// Matches actual API server request format (camelCase)
data class CreateDiscountRequest(
    val sdkKey: String,
    val appUserId: String,
    val startedAt: String, // ISO 8601 format
    val endedAt: String    // ISO 8601 format
)

// Matches actual API server response format
data class CreateDiscountResponse(
    val discount: AppUserDiscountResponse
)

data class GetDiscountResponse(
    val discount: AppUserDiscountResponse?
)

// API 서버의 실제 응답 형식에 맞춤 (snake_case)
data class AppUserDiscountResponse(
    val id: Int,
    val started_at: String,  // ISO 8601 format string
    val ended_at: String,    // ISO 8601 format string
    val app_user_id: String,
    val sdk_key: String,
    val created_at: String   // ISO 8601 format string
) {
    fun toAppUserDiscount(): AppUserDiscount {
        // Parse dates using ThreeTenABP library for better ISO 8601 support
        return AppUserDiscount(
            id = id,
            appUserId = app_user_id,
            sdkKey = sdk_key,
            startedAt = parseISO8601Date(started_at) ?: throw IllegalArgumentException("Failed to parse started_at: $started_at"),
            endedAt = parseISO8601Date(ended_at) ?: throw IllegalArgumentException("Failed to parse ended_at: $ended_at"),
            createdAt = parseISO8601Date(created_at) ?: throw IllegalArgumentException("Failed to parse created_at: $created_at")
        )
    }
    
    private fun parseISO8601Date(dateString: String): java.util.Date? {
        return DateTimeHelper.parseISO8601(dateString)
    }
}

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