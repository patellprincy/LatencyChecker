@file:Suppress("DEPRECATION")
package com.example.latencychecker

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UsageStatsHelper {

    suspend fun getAppDataUsage(
        context: Context,
        startTime: Long,
        endTime: Long
    ): List<AppDataUsage> = withContext(Dispatchers.IO) {

        val pm = context.packageManager
        val nsm = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

        data class Tot(var wifi: Long = 0L, var mobile: Long = 0L)
        val totals = HashMap<Int, Tot>(256)

        fun add(uid: Int, addWifi: Long, addMobile: Long) {
            val t = totals.getOrPut(uid) { Tot() }
            t.wifi += addWifi
            t.mobile += addMobile
        }

        // ---- Collect per-UID totals (Wi-Fi + Mobile; mobile may be limited on some OEMs) ----
        runCatching {
            nsm.querySummary(ConnectivityManager.TYPE_WIFI, null, startTime, endTime).use { s ->
                val b = NetworkStats.Bucket()
                while (s.hasNextBucket()) {
                    s.getNextBucket(b)
                    add(b.uid, b.rxBytes + b.txBytes, 0L)
                }
            }
        }
        runCatching {
            nsm.querySummary(ConnectivityManager.TYPE_MOBILE, null, startTime, endTime).use { s ->
                val b = NetworkStats.Bucket()
                while (s.hasNextBucket()) {
                    s.getNextBucket(b)
                    add(b.uid, 0L, b.rxBytes + b.txBytes)
                }
            }
        }

        // ---- Map UID -> real package/app info safely ----
        val out = ArrayList<AppDataUsage>(totals.size)

        fun isOverlayPackage(name: String): Boolean =
            name.contains("auto_generated_rro", ignoreCase = true) ||
                    name.startsWith("com.android.rro.", ignoreCase = true)

        fun getAppInfoSafe(pkg: String): ApplicationInfo? =
            if (Build.VERSION.SDK_INT >= 33)
                runCatching { pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0)) }.getOrNull()
            else
                runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()

        for ((uid, t) in totals) {
            val total = t.wifi + t.mobile
            if (total <= 0L) continue

            val candidates = pm.getPackagesForUid(uid)?.toList().orEmpty()
            var ai: ApplicationInfo? = null
            var pkgName: String? = null

            // Try non-overlay candidates first
            for (candidate in candidates) {
                if (isOverlayPackage(candidate)) continue
                ai = getAppInfoSafe(candidate)
                if (ai != null) { pkgName = candidate; break }
            }
            // If none resolved, try all candidates (still may resolve)
            if (ai == null) {
                for (candidate in candidates) {
                    ai = getAppInfoSafe(candidate)
                    if (ai != null) { pkgName = candidate; break }
                }
            }
            // Final fallback: if we still can't resolve, use the first package name as label
            if (pkgName == null) {
                pkgName = candidates.firstOrNull() ?: continue
            }

            val appName = ai?.let { pm.getApplicationLabel(it).toString() } ?: pkgName
            val icon = ai?.let { pm.getApplicationIcon(it) } ?: pm.defaultActivityIcon

            out += AppDataUsage(
                appName = appName,
                packageName = pkgName,
                appIcon = icon,
                wifiBytes = t.wifi,
                mobileBytes = t.mobile,
                totalBytes = total
            )
        }

        out.sortByDescending { it.totalBytes }
        out
    }
}
