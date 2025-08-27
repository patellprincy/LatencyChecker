package com.example.latencychecker.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import kotlin.math.max

object SpeedTestRunner {
    private val client = OkHttpClient.Builder().build()

    suspend fun runPing(host: String = "www.google.com", attempts: Int = 5): Long =
        withContext(Dispatchers.IO) {
            var best = Long.MAX_VALUE
            repeat(attempts) {
                val t0 = System.nanoTime()
                runCatching { InetAddress.getByName(host).isReachable(1500) }.getOrDefault(true)
                val ms = (System.nanoTime() - t0) / 1_000_000
                best = minOf(best, ms)
            }
            if (best == Long.MAX_VALUE) 0 else best
        }

    /** Simple HTTP download; returns Mbps */
    suspend fun runDownload(bytes: Long = 5_000_000): Double = withContext(Dispatchers.IO) {
        val url = "https://speed.cloudflare.com/__down?bytes=$bytes"
        val req = Request.Builder().url(url).build()
        val t0 = System.nanoTime()
        client.newCall(req).execute().use { resp -> resp.body?.bytes() }
        val dt = max(1L, (System.nanoTime() - t0))
        val bits = bytes * 8.0
        (bits / (dt / 1_000_000_000.0)) / 1_000_000.0
    }
}
