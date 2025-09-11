package com.monetai.sdk.network

import com.monetai.sdk.models.*
import com.monetai.sdk.utils.DateTimeHelper
import com.google.gson.annotations.SerializedName
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
    @SerializedName("sdkKey") val sdkKey: String,
    @SerializedName("platform") val platform: String = "android",
    @SerializedName("version") val version: String
)

// AB Test API request/response models
data class ABTestRequest(
    @SerializedName("sdkKey") val sdkKey: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("platform") val platform: String = "android"
)

// Matches actual API server response format
data class ABTestResponse(
    @SerializedName("group") val group: String?,  // 'baseline' | 'monetai' | 'unknown' | null
    @SerializedName("campaign") val campaign: CampaignResponse?
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
    @SerializedName("id") val id: Int,
    @SerializedName("created_at") val created_at: String?,  // ISO 8601 format string
    @SerializedName("organization_id") val organization_id: Int,
    @SerializedName("campaign_name") val campaign_name: String,
    @SerializedName("started_at") val started_at: String?,  // ISO 8601 format string
    @SerializedName("ended_at") val ended_at: String?,    // ISO 8601 format string
    @SerializedName("traffic_ratio") val traffic_ratio: Double,
    @SerializedName("allocation_ratio") val allocation_ratio: Double,
    @SerializedName("discount_ratio") val discount_ratio: Double,
    @SerializedName("exposure_time_sec") val exposure_time_sec: Int,
    @SerializedName("model_accuracy") val model_accuracy: Double?
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
    @SerializedName("organization_id") val organization_id: Int,  // changed to snake_case
    @SerializedName("platform") val platform: String,
    @SerializedName("version") val version: String
)

data class CreateEventRequest(
    @SerializedName("sdkKey") val sdkKey: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("eventName") val eventName: String,
    @SerializedName("params") val params: Map<String, Any>?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("platform") val platform: String = "android"
)

data class PredictRequest(
    @SerializedName("sdkKey") val sdkKey: String,
    @SerializedName("userId") val userId: String
)

// Matches actual API server response format
data class PredictApiResponse(
    @SerializedName("prediction") val prediction: String?,  // can be null
    @SerializedName("testGroup") val testGroup: String?    // can be null
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
    @SerializedName("sdkKey") val sdkKey: String,
    @SerializedName("appUserId") val appUserId: String,
    @SerializedName("startedAt") val startedAt: String, // ISO 8601 format
    @SerializedName("endedAt") val endedAt: String    // ISO 8601 format
)

// Matches actual API server response format
data class CreateDiscountResponse(
    @SerializedName("discount") val discount: AppUserDiscountResponse
)

data class GetDiscountResponse(
    @SerializedName("discount") val discount: AppUserDiscountResponse?
)

// API 서버의 실제 응답 형식에 맞춤 (snake_case)
data class AppUserDiscountResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("started_at") val started_at: String,  // ISO 8601 format string
    @SerializedName("ended_at") val ended_at: String,    // ISO 8601 format string
    @SerializedName("app_user_id") val app_user_id: String,
    @SerializedName("sdk_key") val sdk_key: String,
    @SerializedName("created_at") val created_at: String   // ISO 8601 format string
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
    @SerializedName("purchaseToken") val purchaseToken: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("sdkKey") val sdkKey: String
)

data class PurchaseHistoryRequest(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("sdkKey") val sdkKey: String,
    @SerializedName("purchases") val purchases: List<PurchaseItem>
)

data class PurchaseItem(
    @SerializedName("purchaseToken") val purchaseToken: String
)

class EmptyResponse() 