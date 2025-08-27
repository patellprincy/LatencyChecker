package com.example.latencychecker.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object DeviceScanner {
    data class Device(val ip: String, val mac: String)

    suspend fun scanArp(): List<Device> = withContext(Dispatchers.IO) {
        val f = File("/proc/net/arp")
        if (!f.exists()) return@withContext emptyList()
        f.readLines().drop(1).mapNotNull { line ->
            val parts = line.split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (parts.size >= 6 && parts[3].matches("..:..:..:..:..:..".toRegex())) {
                Device(parts[0], parts[3])
            } else null
        }
    }
}
