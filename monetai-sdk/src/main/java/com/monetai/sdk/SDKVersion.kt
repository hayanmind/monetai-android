package com.monetai.sdk

/**
 * SDK version information
 */
object SDKVersion {
    private const val VERSION = "1.1.1"
    
    @JvmStatic
    fun getVersion(): String = VERSION
} 