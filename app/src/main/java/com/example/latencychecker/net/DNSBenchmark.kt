package com.example.latencychecker.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.math.max

object DnsBenchmark {

    data class Result(
        val name: String,
        val addr: String,        // e.g., 1.1.1.1
        val medianMs: Long,      // -1 if all attempts failed
        val samples: List<Long>, // individual timings
        val method: String       // "DoH" or "TCP:53"
    )

    private val client: OkHttpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .callTimeout(2, TimeUnit.SECONDS)
        .connectTimeout(1200, TimeUnit.MILLISECONDS)
        .build()

    private data class Target(val name: String, val dohUrl: String, val ip: String)
    private val targets = listOf(
        Target("Cloudflare", "https://cloudflare-dns.com/dns-query?name=example.com&type=A", "1.1.1.1"),
        Target("Google DNS", "https://dns.google/resolve?name=example.com&type=A", "8.8.8.8"),
        Target("Quad9",      "https://dns.quad9.net:5053/dns-query?name=example.com&type=A", "9.9.9.9")
    )

    suspend fun benchmark(rounds: Int = 5, timeoutMs: Int = 1500): List<Result> =
        withContext(Dispatchers.IO) {
            targets.map { t ->
                val dohTimes = mutableListOf<Long>()
                repeat(rounds) {
                    val ms = measureDoH(t.dohUrl)
                    if (ms > 0) dohTimes += ms
                }

                if (dohTimes.isNotEmpty()) {
                    Result(t.name, t.ip, median(dohTimes), dohTimes, "DoH")
                } else {
                    val tcpTimes = mutableListOf<Long>()
                    repeat(rounds) {
                        val ms = tcpConnect(t.ip, 53, timeoutMs)
                        if (ms > 0) tcpTimes += ms
                    }
                    Result(t.name, t.ip, if (tcpTimes.isNotEmpty()) median(tcpTimes) else -1, tcpTimes, "TCP:53")
                }
            }.sortedBy { if (it.medianMs < 0) Long.MAX_VALUE else it.medianMs }
        }

    private fun measureDoH(url: String): Long {
        return runCatching {
            val t0 = System.nanoTime()
            val req = Request.Builder()
                .url(url)
                .header("accept", "application/dns-json")
                .build()
            client.newCall(req).execute().use { /* ignore body; timing only */ }
            max(1, (System.nanoTime() - t0) / 1_000_000)
        }.getOrElse { -1L }
    }

    private fun tcpConnect(host: String, port: Int, timeoutMs: Int): Long {
        return runCatching {
            val t0 = System.nanoTime()
            Socket().use { s ->
                s.setSoLinger(false, 0)
                s.tcpNoDelay = true
                s.connect(InetSocketAddress(host, port), timeoutMs)
            }
            max(1, (System.nanoTime() - t0) / 1_000_000)
        }.getOrElse { -1L }
    }

    private fun median(values: List<Long>): Long {
        if (values.isEmpty()) return -1
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }
}
