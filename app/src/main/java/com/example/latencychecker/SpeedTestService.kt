package com.example.latencychecker.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.system.measureTimeMillis

object SpeedTestService {

    suspend fun measureDownloadSpeed(): Pair<Float, Long> = withContext(Dispatchers.IO) {
        val testUrl = "https://speedtest.tele2.net/10MB.zip"
        val buffer = ByteArray(1024)
        var totalBytes = 0

        val time = measureTimeMillis {
            try {
                val url = URL(testUrl)
                val conn = url.openConnection() as HttpsURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.connect()

                conn.inputStream.use { input: InputStream ->
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        totalBytes += bytesRead
                        if (totalBytes >= 1024 * 1024 * 5) break // 5MB max
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("SpeedTest", "Download error: ${e.localizedMessage}")
            }
        }

        val speedMbps = if (time > 0) {
            (totalBytes * 8f / (1000 * 1000)) / (time / 1000f)
        } else {
            0f
        }

        Log.d("SpeedTest", "Downloaded $totalBytes bytes in $time ms ($speedMbps Mbps)")
        speedMbps to time
    }
}
