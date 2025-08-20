package com.monetai.sdk

/**
 * SDK version information
 */
object SDKVersion {
    private const val VERSION = "1.0.2"
    
    @JvmStatic
    fun getVersion(): String = VERSION
} 