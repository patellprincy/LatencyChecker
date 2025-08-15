package com.example.latencychecker

interface UsageRepo {
    suspend fun getWindow(start: Long, end: Long): List<AppDataUsage>
    suspend fun saveSnapshot(apps: List<AppDataUsage>, ts: Long)
}

class UsageRepoImpl(
    private val helper: UsageStatsHelper,
    private val dao: UsageSnapshotDao
) : UsageRepo {

    override suspend fun getWindow(start: Long, end: Long): List<AppDataUsage> {
        // use your existing helper that queries NetworkStatsManager
        return helper.getAppDataUsageUnsafe(start, end)
    }

    override suspend fun saveSnapshot(apps: List<AppDataUsage>, ts: Long) {
        val rows = apps.map { UsageSnapshot(ts = ts, packageName = it.packageName, totalBytes = it.totalBytes) }
        dao.insertAll(rows)
    }
}
