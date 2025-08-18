package com.example.latencychecker

import android.graphics.drawable.Drawable

/**
 * Canonical model used everywhere. Make sure there is no other duplicate
 * class named AppDataUsage anywhere in the project.
 */
data class AppDataUsage(
    val appName: String,
    val packageName: String,
    val appIcon: Drawable,
    val wifiBytes: Long,
    val mobileBytes: Long,
    val totalBytes: Long
)
