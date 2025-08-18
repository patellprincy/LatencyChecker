package com.example.latencychecker.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "usage_snapshots")
data class UsageSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val dataUsed: Long,
    val timestamp: Long
)

data class DayTotal(val day: String, val total: Long)
data class AppTotal(val packageName: String, val total: Long)

@Dao
interface UsageSnapDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: UsageSnapshot)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(snapshots: List<UsageSnapshot>) // Added for old code support

    @Query("SELECT * FROM usage_snapshots WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getUsageBetween(start: Long, end: Long): Flow<List<UsageSnapshot>>

    @Query("""
        SELECT strftime('%Y-%m-%d', datetime(timestamp/1000,'unixepoch')) as day,
               SUM(dataUsed) as total
        FROM usage_snapshots
        WHERE timestamp BETWEEN :start AND :end
        GROUP BY day ORDER BY day
    """)
    suspend fun sumPerDay(start: Long, end: Long): List<DayTotal>

    @Query("""
        SELECT appName as packageName, SUM(dataUsed) as total
        FROM usage_snapshots
        WHERE timestamp BETWEEN :start AND :end
        GROUP BY appName ORDER BY total DESC LIMIT 5
    """)
    suspend fun topApps(start: Long, end: Long): List<AppTotal>

    // For old references
    @Query("SELECT * FROM usage_snapshots")
    suspend fun getAppDataUsage(): List<UsageSnapshot>

    @Query("SELECT * FROM usage_snapshots")
    fun getAppDataUsageUnsafe(): List<UsageSnapshot>
}
