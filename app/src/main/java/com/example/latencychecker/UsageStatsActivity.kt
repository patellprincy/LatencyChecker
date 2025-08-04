package com.example.latencychecker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.latencychecker.adapter.AppUsageAdapter
import com.example.latencychecker.util.UsageStatsHelper

class UsageStatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage_stats)

        // Check usage stats permission
        if (!hasUsageStatsPermission(this)) {
            requestUsageStatsPermission(this)
            return
        }

        // Retrieve usage stats and set up RecyclerView
        val usageStats = UsageStatsHelper.getAppUsageStats(this)
        val recyclerView = findViewById<RecyclerView>(R.id.rvUsageStats)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AppUsageAdapter(usageStats, packageManager)
    }
}
