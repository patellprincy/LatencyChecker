package com.example.latencychecker

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.latencychecker.data.local.UsageSnapDao
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class AppUsageViewModel(private val dao: UsageSnapDao) : ViewModel() {

    // Hourly usage for charts
    val hourlyUsage: LiveData<List<Entry>> =
        dao.getUsageBetween(
            start = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1),
            end = System.currentTimeMillis()
        ).map { snapshots ->
            snapshots.map {
                Entry(it.timestamp.toFloat(), it.dataUsed.toFloat())
            }
        }.asLiveData()
}
