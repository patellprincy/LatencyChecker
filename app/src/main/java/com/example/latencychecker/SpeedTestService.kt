package com.example.latencychecker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.system.measureTimeMillis

object SpeedTestService {
    private val client = OkHttpClient()

    suspend fun measureDownloadSpeed(): Pair<Float, Long> = withContext(Dispatchers.IO) {
        val testUrl = "https://speed.cloudflare.com/__down?bytes=5242880" // ~5MB
        var totalBytes = 0
        val buffer = ByteArray(8 * 1024) // 8 KB buffer

        try {
            val request = Request.Builder().url(testUrl).build()
            val time = measureTimeMillis {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception("HTTP error: ${response.code}")
                    }

                    response.body?.byteStream()?.use { input ->
                        while (true) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) break
                            totalBytes += bytesRead
                            if (totalBytes >= 1024 * 1024 * 5) break // Limit to 5 MB
                        }
                    }
                }
            }

            val speedMbps = (totalBytes * 8f / (1000 * 1000)) / (time / 1000f)
            speedMbps to time
        } catch (e: Exception) {
            e.printStackTrace()
            0f to 0L
        }
    }
}
