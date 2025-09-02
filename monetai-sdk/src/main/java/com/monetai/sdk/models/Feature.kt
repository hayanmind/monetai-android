package com.monetai.sdk.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Feature model for paywall
 */
@Parcelize
data class Feature(
    val title: String,                  // Feature title
    val description: String,            // Feature description
    val isPremiumOnly: Boolean = false // Whether this feature is premium only
) : Parcelable
