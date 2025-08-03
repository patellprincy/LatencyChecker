package com.example.latencychecker

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.latencychecker.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DashboardViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvNetwork = findViewById<TextView>(R.id.tvNetworkType)
        val tvSpeed = findViewById<TextView>(R.id.tvDownloadSpeed)
        val tvLatency = findViewById<TextView>(R.id.tvDnsLatency)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)

        lifecycleScope.launchWhenStarted {
            viewModel.state.collectLatest { state ->
                tvNetwork.text = "Network Type: ${state.networkType}"
                tvSpeed.text = "Download Speed: ${state.downloadSpeedMbps} Mbps"
                tvLatency.text = "DNS Latency: ${state.dnsLatencyMs} ms"
            }
        }

        btnRefresh.setOnClickListener {
            viewModel.refreshNetworkStats()
        }
    }
}
