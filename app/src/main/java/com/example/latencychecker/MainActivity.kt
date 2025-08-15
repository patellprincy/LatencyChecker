package com.example.latencychecker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.latencychecker.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModel()

    private lateinit var tvNetwork: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvLatency: TextView
    private lateinit var rvUsageStats: RecyclerView
    private val usageAdapter = AppUsageAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        observeState()

        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        } else {
            loadAppUsageData()
        }
    }

    private fun initViews() {
        tvNetwork = findViewById(R.id.tvNetworkType)
        tvSpeed = findViewById(R.id.tvDownloadSpeed)
        tvLatency = findViewById(R.id.tvDnsLatency)

        rvUsageStats = findViewById(R.id.rvUsageStats)
        rvUsageStats.layoutManager = LinearLayoutManager(this)
        rvUsageStats.adapter = usageAdapter
        rvUsageStats.setHasFixedSize(true)
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                tvNetwork.text = "Network Type: ${state.networkType}"
                tvSpeed.text = "Download Speed: %.2f Mbps".format(state.downloadSpeedMbps)
                tvLatency.text = "DNS Latency: ${state.dnsLatencyMs} ms"
            }
        }
    }

    private fun loadAppUsageData() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)

        lifecycleScope.launch(Dispatchers.IO) {
            val data = UsageStatsHelper.getAppDataUsage(this@MainActivity, startTime, endTime)
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
}
