package com.example.latencychecker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.latencychecker.databinding.ActivityUsageStatsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.max

class UsageStatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageStatsBinding
    private val adapter = AppUsageAdapter()

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadAndRender()
            handler.postDelayed(this, 30_000)
        }
    }

    // Data cache for filtering/sorting
    private var fullList: List<AppDataUsage> = emptyList()
    private var currentList: List<AppDataUsage> = emptyList()

    private enum class SortBy { TOTAL, WIFI, MOBILE, NAME }
    private var sortBy: SortBy = SortBy.TOTAL

    // timeframe (default 24h)
    private var timeframeMillis: Long = TimeUnit.HOURS.toMillis(24)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsageStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setTitleTextColor(android.graphics.Color.WHITE)

        // RecyclerView
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)

        // Pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadAndRender()
        }

        // Search filter
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFiltersAndSort()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Timeframe chips
        binding.timeframeChips.setOnCheckedChangeListener { _, checkedId ->
            timeframeMillis = when (checkedId) {
                binding.chip1h.id -> TimeUnit.HOURS.toMillis(1)
                binding.chip24h.id -> TimeUnit.HOURS.toMillis(24)
                binding.chip7d.id -> TimeUnit.DAYS.toMillis(7)
                else -> TimeUnit.HOURS.toMillis(24)
            }
            loadAndRender()
        }

        // Sort menu
        binding.sortButton.setOnClickListener { anchor ->
            PopupMenu(this, anchor).apply {
                menu.add("Sort by Total")
                menu.add("Sort by Wi-Fi")
                menu.add("Sort by Mobile")
                menu.add("Sort by Name")
                setOnMenuItemClickListener {
                    sortBy = when (it.title.toString()) {
                        "Sort by Wi-Fi" -> SortBy.WIFI
                        "Sort by Mobile" -> SortBy.MOBILE
                        "Sort by Name" -> SortBy.NAME
                        else -> SortBy.TOTAL
                    }
                    applyFiltersAndSort()
                    true
                }
                show()
            }
        }

        // Permission gate
        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
            Toast.makeText(this, "Please grant Usage Access and return.", Toast.LENGTH_SHORT).show()
        } else {
            loadAndRender()
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasUsageStatsPermission()) handler.post(refreshRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun loadAndRender() {
        val endTime = System.currentTimeMillis()
        val startTime = max(0L, endTime - timeframeMillis)

        lifecycleScope.launch(Dispatchers.IO) {
            val data = UsageStatsHelper.getAppDataUsage(
                context = this@UsageStatsActivity,
                startTime = startTime,
                endTime = endTime
            )

            withContext(Dispatchers.Main) {
                fullList = data
                applyFiltersAndSort()
                binding.swipeRefresh.isRefreshing = false

                // Optional: update header metrics if you have them elsewhere
                // binding.networkTypeText.text = "Network Type: ${NetworkUtils.getType(this@UsageStatsActivity)}"
                // binding.downloadSpeedText.text = "Download Speed: ${SpeedTestService.latestMbps} Mbps"
                // binding.dnsLatencyText.text = "DNS Latency: ${DnsLatencyChecker.latestMs} ms"
            }
        }
    }

    private fun applyFiltersAndSort() {
        val q = binding.searchInput.text?.toString()?.trim()?.lowercase().orEmpty()

        // filter
        var filtered = if (q.isEmpty()) fullList else fullList.filter {
            it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }

        // sort
        filtered = when (sortBy) {
            SortBy.TOTAL -> filtered.sortedByDescending { it.totalBytes }
            SortBy.WIFI  -> filtered.sortedByDescending { it.wifiBytes }
            SortBy.MOBILE-> filtered.sortedByDescending { it.mobileBytes }
            SortBy.NAME  -> filtered.sortedBy { it.appName.lowercase() }
        }

        currentList = filtered
        adapter.submit(currentList)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}
