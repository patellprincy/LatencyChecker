package com.example.latencychecker

import android.graphics.drawable.Drawable

data class AppDataUsage(
    val appName: String,
    val packageName: String,
    val appIcon: Drawable,
    val wifiBytes: Long,
    val mobileBytes: Long,
    val totalBytes: Long
)
