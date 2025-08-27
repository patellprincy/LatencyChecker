package com.example.latencychecker.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object DnsBenchmark {
    private val client = OkHttpClient.Builder().build()

    data class Result(val name: String, val ms: Long)

    suspend fun benchmark(): List<Result> = withContext(Dispatchers.IO) {
        val targets = listOf(
            "https://1.1.1.1" to "Cloudflare",
            "https://8.8.8.8" to "Google DNS"
        )

        targets.map { (url, name) ->
            val t0 = System.nanoTime()
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().close()
            }
            val ms = (System.nanoTime() - t0) / 1_000_000
            Result(name, ms)
        }
    }
}
