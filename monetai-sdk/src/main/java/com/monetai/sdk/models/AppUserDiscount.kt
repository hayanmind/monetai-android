package com.monetai.sdk.models

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * App user discount information
 */
data class AppUserDiscount(
    val id: Int,
    @SerializedName("app_user_id")
    val appUserId: String,
    @SerializedName("sdk_key")
    val sdkKey: String,
    @SerializedName("started_at")
    val startedAt: Date,
    @SerializedName("ended_at")
    val endedAt: Date,
    @SerializedName("created_at")
    val createdAt: Date
) 