package com.example.latencychecker.util

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import java.util.*

object UsageStatsHelper {
    fun getAppUsageStats(context: Context): List<UsageStats> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (1000 * 60 * 60 * 24) // Last 24 hours

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        Log.d("UsageStats", "Fetched stats count: ${stats.size}")
        stats.forEach {
            Log.d("UsageStats", "${it.packageName} - Time: ${it.totalTimeInForeground}")
        }

        return stats.filter { it.totalTimeInForeground > 0 }
    }
}
