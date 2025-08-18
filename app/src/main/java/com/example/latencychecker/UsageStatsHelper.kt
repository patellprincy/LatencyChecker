package com.example.latencychecker

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.RemoteException
import android.util.Log
import com.example.latencychecker.data.local.UsageSnapDao
import com.example.latencychecker.data.local.UsageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UsageStatsHelper {
    private lateinit var appContext: Context
    private lateinit var dao: UsageSnapDao

    fun init(context: Context, dao: UsageSnapDao) {
        appContext = context.applicationContext
        this.dao = dao
    }

    /** New function so MainActivity can directly get usage list */
    suspend fun getAppDataUsage(
        context: Context,
        startTime: Long,
        endTime: Long
    ): List<AppDataUsage> = withContext(Dispatchers.IO) {
        fetchRealData(context, startTime, endTime)
    }

    suspend fun getAppDataUsageAndStore(startTime: Long, endTime: Long) {
        val usageList = fetchRealData(appContext, startTime, endTime)
        val timestamp = System.currentTimeMillis()

        withContext(Dispatchers.IO) {
            usageList.forEach { usage ->
                dao.insert(
                    UsageSnapshot(
                        appName = usage.appName,
                        dataUsed = usage.totalBytes,
                        timestamp = timestamp
                    )
                )
            }
        }
    }

    private fun fetchRealData(
        context: Context,
        startTime: Long,
        endTime: Long
    ): List<AppDataUsage> {
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val packageManager = context.packageManager
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val appList = mutableListOf<AppDataUsage>()

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

                    appList.add(AppDataUsage(appName, packageName, appIcon, wifiBytes, mobileBytes, totalBytes))
                }
            } catch (e: Exception) {
                Log.e("UsageStatsHelper", "Error fetching usage for $packageName", e)
            }
        }
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
