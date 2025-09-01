package com.example.latencychecker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
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
    private lateinit var tvDnsFastest: TextView
    private lateinit var btnDnsBenchmark: Button

    private lateinit var rvUsageStats: RecyclerView
    private val usageAdapter = AppUsageAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        observeState()

        // Usage access gate
        if (hasUsageStatsPermission()) {
            loadAppUsageData()
        } else {
            requestUsageStatsPermission()
        }

        // DNS benchmark button
        btnDnsBenchmark.setOnClickListener { runDnsBenchmarkAndShow() }
    }

    private fun initViews() {
        tvNetwork = findViewById(R.id.tvNetworkType)
        tvSpeed   = findViewById(R.id.tvDownloadSpeed)
        tvLatency = findViewById(R.id.tvDnsLatency)

        tvDnsFastest   = findViewById(R.id.tvDnsFastest)
        btnDnsBenchmark = findViewById(R.id.btnDnsBenchmark)

        rvUsageStats = findViewById(R.id.rvUsageStats)
        rvUsageStats.layoutManager = LinearLayoutManager(this)
        rvUsageStats.adapter = usageAdapter
        rvUsageStats.setHasFixedSize(true)
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                tvNetwork.text = "Network Type: ${state.networkType}"
                tvSpeed.text   = "Download Speed: %.2f Mbps".format(state.downloadSpeedMbps)
                tvLatency.text = "DNS Latency: ${state.dnsLatencyMs} ms"
            }
        }
    }

    private fun loadAppUsageData() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(1)

        lifecycleScope.launch(Dispatchers.IO) {
            val data = UsageStatsHelper.getAppDataUsage(
                context = this@MainActivity,
                startTime = startTime,
                endTime = endTime
            )
            withContext(Dispatchers.Main) {
                usageAdapter.submit(data)
            }
        }
    }

    /** Runs DNS benchmark (DoH first, TCP:53 fallback) and shows a dialog with results. */
    private fun runDnsBenchmarkAndShow() {
        btnDnsBenchmark.isEnabled = false
        val oldText = btnDnsBenchmark.text
        btnDnsBenchmark.text = "Testing…"

        lifecycleScope.launch {
            val results = withContext(Dispatchers.IO) {
                // Use your existing object path
                com.example.latencychecker.net.DnsBenchmark.benchmark()
            }

            btnDnsBenchmark.isEnabled = true
            btnDnsBenchmark.text = oldText

            // Fastest successful
            val fastest = results.firstOrNull { it.medianMs >= 0 }
            tvDnsFastest.text = fastest?.let {
                "Fastest DNS: ${it.name} (${it.addr}) • ${it.medianMs} ms"
            } ?: "Fastest DNS: — (unreachable)"

            // Pretty summary for dialog
            val summary = buildString {
                results.forEach { r ->
                    appendLine(
                        "${r.name} (${r.addr}) — " +
                                (if (r.medianMs >= 0) "${r.medianMs} ms [${r.method}]"
                                else "unreachable")
                    )
                }
            }

            AlertDialog.Builder(this@MainActivity)
                .setTitle("DNS Benchmark")
                .setMessage(summary.trim())
                .setPositiveButton("OK", null)
                .show()
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
