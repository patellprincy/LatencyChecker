package com.example.latencychecker.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object DnsLatencyChecker {

    suspend fun pingHost(host: String = "google.com"): Long = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("/system/bin/ping", "-c", "1", host))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

            process.waitFor()

            val matcher = Regex("time=(\\d+(\\.\\d+)?)").find(output.toString())
            val latency = matcher?.groupValues?.get(1)?.toDoubleOrNull()?.toLong() ?: -1L

            Log.d("DNS", "Ping output: $output")
            Log.d("DNS", "Latency: $latency ms")

            return@withContext latency
        } catch (e: Exception) {
            Log.e("DNS", "Ping failed: ${e.localizedMessage}")
            return@withContext -1L
        }
    }
}
