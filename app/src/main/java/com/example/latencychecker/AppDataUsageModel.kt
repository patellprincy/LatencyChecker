package com.example.latencychecker.model

import android.graphics.drawable.Drawable

data class AppDataUsage(
    val appName: String,              // User-friendly app name
    val appIcon: Drawable,            // Icon drawable
    val wifiBytes: Long,              // Wi-Fi data usage in bytes
    val mobileBytes: Long,            // Mobile data usage in bytes
    val totalBytes: Long               // Total (Wi-Fi + Mobile)
)
