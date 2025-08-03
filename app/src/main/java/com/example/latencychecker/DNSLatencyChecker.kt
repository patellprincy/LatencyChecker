package com.example.latencychecker.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object DnsLatencyChecker {

    suspend fun pingHost(host: String): Long = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 $host")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            process.waitFor()

            // Extract latency value from output
            val matcher = Regex("time=(\\d+(\\.\\d+)?)").find(output)
            return@withContext matcher?.groupValues?.get(1)?.toDoubleOrNull()?.toLong() ?: -1L
        } catch (e: Exception) {
            return@withContext -1L
        }
    }
}
