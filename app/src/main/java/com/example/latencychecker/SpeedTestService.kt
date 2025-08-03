package com.example.latencychecker.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import kotlin.system.measureTimeMillis

object SpeedTestService {

    suspend fun measureDownloadSpeed(): Pair<Float, Long> = withContext(Dispatchers.IO) {
        val testUrl = "https://speed.hetzner.de/100MB.bin" // test file
        val buffer = ByteArray(1024)
        val url = URL(testUrl)
        var totalBytes = 0

        val time = measureTimeMillis {
            url.openStream().use { input ->
                while (true) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break
                    totalBytes += bytesRead
                    if (totalBytes >= 1024 * 1024 * 5) break // limit to ~5MB
                }
            }
        }

        val speedMbps = (totalBytes * 8f / (1000 * 1000)) / (time / 1000f) // Mbps
        speedMbps to time
    }
}
