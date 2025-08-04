package com.example.latencychecker

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.latencychecker.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModel()

    private lateinit var tvNetwork: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvLatency: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        observeState()
        requestUsageStatsPermission(this)
    }

    private fun initViews() {
        tvNetwork = findViewById(R.id.tvNetworkType)
        tvSpeed = findViewById(R.id.tvDownloadSpeed)
        tvLatency = findViewById(R.id.tvDnsLatency)
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
}
