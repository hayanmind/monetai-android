package com.monetai.sdk.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.ResponseBody
import retrofit2.Converter
import java.lang.reflect.Type
import com.monetai.sdk.SDKVersion

/**
 * Custom converter that handles empty response bodies.
 * - For EmptyResponse type: always returns EmptyResponse()
 * - For other types: returns null if body is empty, delegates to next converter otherwise
 */
class NullOnEmptyConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        if (type == EmptyResponse::class.java) {
            return Converter<ResponseBody, EmptyResponse> { EmptyResponse() }
        }
        val delegate = retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
        return Converter<ResponseBody, Any?> { body ->
            if (body.contentLength() == 0L) null else delegate.convert(body)
        }
    }
}

/**
 * SDK header interceptor for adding platform and version headers to all requests
 */
class SDKHeaderInterceptor : Interceptor {
    var appVersion: String = ""
    var packageName: String = ""
    var userId: String = ""

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-SDK-Platform", "android")
            .addHeader("X-SDK-Version", SDKVersion.getVersion())
            .addHeader("X-Device-OS", "android")
            .apply {
                if (appVersion.isNotEmpty()) addHeader("X-App-Version", appVersion)
                if (packageName.isNotEmpty()) addHeader("X-App-Bundle-Id", packageName)
                if (userId.isNotEmpty()) addHeader("X-User-Id", userId)
            }
            .build()
        return chain.proceed(request)
    }
}

/**
 * API client for Monetai SDK
 */
object ApiClient {
    private const val BASE_URL = "https://monetai-api-414410537412.us-central1.run.app/sdk/"

    internal val sdkHeaderInterceptor = SDKHeaderInterceptor()

    private val gson: Gson = GsonBuilder()
        .create()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(sdkHeaderInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(NullOnEmptyConverterFactory())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
