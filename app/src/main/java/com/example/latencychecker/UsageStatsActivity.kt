package com.example.latencychecker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.latencychecker.databinding.ActivityUsageStatsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class UsageStatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageStatsBinding
    private val usageAdapter = AppUsageAdapter()

    // Handler for periodic updates
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadUsageStats()
            handler.postDelayed(this, 30_000) // 30 seconds
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsageStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Permission check
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
            return
        }

        // Setup RecyclerView
        binding.rvUsageStats.layoutManager = LinearLayoutManager(this)
        binding.rvUsageStats.adapter = usageAdapter
        binding.rvUsageStats.setHasFixedSize(true)

        // Start periodic refresh
        handler.post(refreshRunnable)
    }

    private fun loadUsageStats() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)

        lifecycleScope.launch(Dispatchers.IO) {
            val data = UsageStatsHelper.getAppDataUsage(this@UsageStatsActivity, startTime, endTime)
            withContext(Dispatchers.Main) {
                usageAdapter.submit(data)
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable) // stop updates when activity is destroyed
    }
}
