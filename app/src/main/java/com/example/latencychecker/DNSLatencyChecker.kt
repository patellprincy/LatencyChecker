package com.example.latencychecker.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.max

object DnsLatencyChecker {

    suspend fun pingHost(host: String = "google.com", timeoutMs: Int = 1500): Long =
        withContext(Dispatchers.IO) {
            val icmp = runCatching { icmpPing(host) }.getOrDefault(-1L)
            if (icmp > 0) return@withContext icmp
            tcpRtt(host, 53, timeoutMs)
        }

    private fun icmpPing(host: String): Long {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("/system/bin/ping", "-c", "1", host))
            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            val out = buildString {
                var line: String?
                while (reader.readLine().also { line = it } != null) appendLine(line)
            }
            proc.waitFor()

            val ms = Regex("time=(\\d+(?:\\.\\d+)?)")
                .find(out)?.groupValues?.get(1)?.toDoubleOrNull()?.toLong() ?: -1L

            Log.d("DNS", "ICMP $host -> $ms ms")
            ms
        } catch (e: Exception) {
            Log.e("DNS", "ICMP failed: ${e.localizedMessage}")
            -1L
        }
    }

    private fun tcpRtt(host: String, port: Int, timeoutMs: Int): Long {
        return runCatching {
            val t0 = System.nanoTime()
            Socket().use { s ->
                // disable linger via the setter (property is read-only)
                s.setSoLinger(false, 0)
                s.tcpNoDelay = true
                s.connect(InetSocketAddress(host, port), timeoutMs)
            }
            max(1, (System.nanoTime() - t0) / 1_000_000)
        }.getOrElse {
            Log.e("DNS", "TCP RTT failed: ${it.localizedMessage}")
            -1L
        }
    }
}
