package com.example.latencychecker

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.RemoteException
import android.util.Log

data class AppDataUsage(
    val appName: String,
    val packageName: String,
    val appIcon: android.graphics.drawable.Drawable,
    val wifiBytes: Long,
    val mobileBytes: Long,
    val totalBytes: Long
)

object UsageStatsHelper {

    fun getAppDataUsage(context: Context, startTime: Long, endTime: Long): List<AppDataUsage> {
        val realData = fetchRealData(context, startTime, endTime)

        // If running on emulator or no data found, return mock data for UI testing
        if (realData.isEmpty() || realData.all { it.totalBytes == 0L }) {
            Log.w("UsageStatsHelper", "No real data found — showing mock data for testing")
            return listOf(
                AppDataUsage(
                    "YouTube", "com.google.android.youtube",
                    context.getDrawable(R.mipmap.ic_launcher)!!,
                    500_000_000, 300_000_000, 800_000_000
                ),
                AppDataUsage(
                    "Chrome", "com.android.chrome",
                    context.getDrawable(R.mipmap.ic_launcher)!!,
                    200_000_000, 100_000_000, 300_000_000
                ),
                AppDataUsage(
                    "Instagram", "com.instagram.android",
                    context.getDrawable(R.mipmap.ic_launcher)!!,
                    100_000_000, 50_000_000, 150_000_000
                )
            )
        }

        return realData
    }

    private fun fetchRealData(context: Context, startTime: Long, endTime: Long): List<AppDataUsage> {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val packageManager = context.packageManager
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val appList = mutableListOf<AppDataUsage>()

        // Get only apps that actually have usage data — no QUERY_ALL_PACKAGES
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )
        val usedPackages = usageStats.map { it.packageName }.toSet()

        for (packageName in usedPackages) {
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                val uid = appInfo.uid

                val wifiBytes = getBytesForUid(networkStatsManager, uid, ConnectivityManager.TYPE_WIFI, startTime, endTime)
                val mobileBytes = getBytesForUid(networkStatsManager, uid, ConnectivityManager.TYPE_MOBILE, startTime, endTime)
                val totalBytes = wifiBytes + mobileBytes

                if (totalBytes > 0) {
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val appIcon = packageManager.getApplicationIcon(appInfo)

                    appList.add(
                        AppDataUsage(
                            appName = appName,
                            packageName = packageName,
                            appIcon = appIcon,
                            wifiBytes = wifiBytes,
                            mobileBytes = mobileBytes,
                            totalBytes = totalBytes
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("UsageStatsHelper", "Error fetching usage for $packageName", e)
            }
        }

        // Sort by highest usage first
        return appList.sortedByDescending { it.totalBytes }
    }

    private fun getBytesForUid(
        networkStatsManager: NetworkStatsManager,
        uid: Int,
        networkType: Int,
        startTime: Long,
        endTime: Long
    ): Long {
        return try {
            val bucket = NetworkStats.Bucket()
            val stats = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                networkStatsManager.querySummary(networkType, null, startTime, endTime)
            } else return 0

            var totalBytes = 0L
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                if (bucket.uid == uid) {
                    totalBytes += bucket.rxBytes + bucket.txBytes
                }
            }
            stats.close()
            totalBytes
        } catch (e: RemoteException) {
            e.printStackTrace()
            0
        }
    }
}
