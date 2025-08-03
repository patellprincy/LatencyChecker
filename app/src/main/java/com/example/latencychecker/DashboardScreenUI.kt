package com.example.latencychecker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.latencychecker.viewmodel.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("LatencyChecker Dashboard", style = MaterialTheme.typography.h6)

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            InfoCard(title = "Network Type", value = state.networkType)
            InfoCard(title = "Download Speed", value = "${state.downloadSpeedMbps} Mbps")
            InfoCard(title = "DNS Latency", value = "${state.dnsLatencyMs} ms")

            Button(onClick = { viewModel.refreshNetworkStats() }) {
                Text("Refresh")
            }
        }
    }
}

@Composable
fun InfoCard(title: String, value: String) {
    Card(
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value)
        }
    }
}
