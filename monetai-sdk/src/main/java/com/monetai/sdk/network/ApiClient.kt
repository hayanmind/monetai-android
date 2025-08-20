package com.monetai.sdk.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.ResponseBody
import retrofit2.Converter
import java.lang.reflect.Type
import com.monetai.sdk.utils.DateTimeHelper
import java.util.Date

/**
 * Custom date deserializer for ISO 8601 format with timezone support
 * Uses DateTimeHelper for consistent ISO 8601 parsing
 */
class TimezoneDateDeserializer : JsonDeserializer<Date> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date? {
        return DateTimeHelper.parseISO8601(json?.asString)
    }
}

/**
 * Custom converter for empty responses
 */
class EmptyResponseConverter : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        if (type == EmptyResponse::class.java) {
            return Converter<ResponseBody, EmptyResponse> { responseBody ->
                // Return EmptyResponse instance for any response (including empty)
                EmptyResponse()
            }
        }
        return null
    }
}

/**
 * API client for Monetai SDK
 */
object ApiClient {
    private const val BASE_URL = "https://monetai-api-414410537412.us-central1.run.app/sdk/"
    
    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(java.util.Date::class.java, TimezoneDateDeserializer())
        .create()
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(EmptyResponseConverter())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
} 