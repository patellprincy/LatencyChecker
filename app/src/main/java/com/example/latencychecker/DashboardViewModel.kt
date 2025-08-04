package com.example.latencychecker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.latencychecker.service.DnsLatencyChecker
import com.example.latencychecker.SpeedTestService
import com.example.latencychecker.util.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NetworkStats(
    val networkType: String = "Unknown",
    val downloadSpeedMbps: Float = 0f,
    val dnsLatencyMs: Long = 0L,
    val isLoading: Boolean = true
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(NetworkStats())
    val state: StateFlow<NetworkStats> = _state

    init {
        refreshNetworkStats()
    }

    fun refreshNetworkStats() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val context = getApplication<Application>().applicationContext
            val type = NetworkUtils.getNetworkType(context)
            val latency = DnsLatencyChecker.pingHost("8.8.8.8")
            val (speed, _) = SpeedTestService.measureDownloadSpeed()

            _state.value = NetworkStats(
                networkType = type,
                downloadSpeedMbps = speed,
                dnsLatencyMs = latency,
                isLoading = false
            )
        }
    }
}
