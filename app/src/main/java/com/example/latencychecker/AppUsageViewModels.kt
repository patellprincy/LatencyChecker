package com.example.latencychecker

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AppUsageViewModel(private val dao: UsageSnapDao) : ViewModel() {

    // Hourly usage for the past 1 hour, updated as DB changes
    val hourlyUsage: LiveData<List<Entry>> =
        dao.getUsageBetween(
            start = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1),
            end = System.currentTimeMillis()
        ).map { snapshots ->
            snapshots.map { snapshot ->
                Entry(
                    snapshot.timestamp.toFloat(),
                    snapshot.dataUsed.toFloat()
                )
            }
        }.asLiveData()

    // Top 5 apps for the past 24 hours
    val topAppsDaily: LiveData<List<AppTotal>> =
        dao.getUsageBetween(
            start = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1),
            end = System.currentTimeMillis()
        ).map { snapshots ->
            snapshots.groupBy { it.packageName }
                .map { (pkg, usageList) ->
                    AppTotal(pkg, usageList.sumOf { it.dataUsed })
                }
                .sortedByDescending { it.total }
                .take(5)
        }.asLiveData()

    fun insertSnapshot(snapshot: UsageSnapshot) {
        viewModelScope.launch {
            dao.insert(snapshot)
        }
    }
}
