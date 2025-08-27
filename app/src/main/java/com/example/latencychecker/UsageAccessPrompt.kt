package com.example.latencychecker

import android.app.Activity
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object UsageAccessPrompt {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Show a friendly dialog explaining why, with a single “Allow for LatencyChecker” button. */
    fun showDialog(activity: Activity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Allow Usage Access")
            .setMessage(
                "LatencyChecker needs Usage Access to show per-app Wi-Fi/Mobile data usage. " +
                        "This runs only on your device and isn’t uploaded."
            )
            .setPositiveButton("Allow for LatencyChecker") { _, _ ->
                openUsageAccessSettings(activity)
            }
            .setNegativeButton("Not now", null)
            .setCancelable(true)
            .show()
    }

    /** Best-effort deep links (safe & Play-compliant) */
    private fun openUsageAccessSettings(activity: Activity) {
        // 1) Try usage-access list with a hint for our package (some OEMs respect EXTRA_APP_PACKAGE)
        val usageIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            data = Uri.fromParts("package", activity.packageName, null) // ignored on AOSP, OK elsewhere
        }
        try {
            activity.startActivity(usageIntent); return
        } catch (_: ActivityNotFoundException) { /* fall through */ }

        // 2) Fallback: app details (from there users can find “Usage Access” on some OEMs)
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        try {
            activity.startActivity(details); return
        } catch (_: ActivityNotFoundException) { /* fall through */ }

        // 3) Last resort: open general Settings
        try {
            activity.startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) { /* no-op */ }
    }
}
