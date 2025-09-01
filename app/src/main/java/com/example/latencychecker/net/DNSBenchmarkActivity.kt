package com.example.latencychecker.net

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.latencychecker.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.recyclerview.widget.RecyclerView
import android.widget.ProgressBar

class DnsBenchmarkActivity : AppCompatActivity() {

    private lateinit var list: RecyclerView
    private lateinit var progress: ProgressBar
    private val adapter = DnsResultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dns_benchmark)

        list = findViewById(R.id.list)
        progress = findViewById(R.id.progress)

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        runBenchmark()
    }

    private fun runBenchmark() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val results = DnsBenchmark.benchmark()
            withContext(Dispatchers.Main) {
                progress.visibility = View.GONE
                adapter.submit(results)
            }
        }
    }
}
