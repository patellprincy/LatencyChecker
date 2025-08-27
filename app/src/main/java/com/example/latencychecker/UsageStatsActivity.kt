package com.example.latencychecker

import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.latencychecker.databinding.ActivityUsageStatsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.math.max

class UsageStatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsageStatsBinding
    private val adapter = AppUsageAdapter()

    private val handler = Handler(Looper.getMainLooper())
    private val refresher = object : Runnable {
        override fun run() {
            loadAndRender()
            handler.postDelayed(this, 30_000)
        }
    }

    private enum class SortBy { TOTAL, WIFI, MOBILE, NAME }
    private var sortBy: SortBy = SortBy.TOTAL
    private var timeframeMillis: Long = TimeUnit.HOURS.toMillis(24)
    private var fullList: List<AppDataUsage> = emptyList()

    // track if we already popped the dialog during this resume to avoid double prompts
    private var dialogShownThisResume = false

    // Launcher to know when user returns from Settings
    private val usageAccessLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // When user comes back from Settings, check permission and act accordingly
            if (hasUsageAccess()) {
                Toast.makeText(this, "Usage Access granted", Toast.LENGTH_SHORT).show()
                startRefreshing()
            } else {
                Toast.makeText(this, "Usage Access not granted", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsageStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recycler
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(true)

        // Pull-to-refresh
        binding.swipeRefresh.setOnRefreshListener { loadAndRender() }

        // Search
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = applyFiltersAndSort()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        // Timeframe chips
        binding.timeframeChips.setOnCheckedChangeListener { _, id ->
            timeframeMillis = when (id) {
                binding.chip1h.id -> TimeUnit.HOURS.toMillis(1)
                binding.chip7d.id -> TimeUnit.DAYS.toMillis(7)
                else -> TimeUnit.HOURS.toMillis(24)
            }
            loadAndRender()
        }

        // Sort menu
        binding.sortButton.setOnClickListener { anchor ->
            PopupMenu(this, anchor).apply {
                menu.add("Sort by Total"); menu.add("Sort by Wi-Fi")
                menu.add("Sort by Mobile"); menu.add("Sort by Name")
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
    }

    override fun onStart() {
        super.onStart()
        dialogShownThisResume = false
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refresher)

        if (hasUsageAccess()) {
            startRefreshing()
        } else {
            // ALWAYS show the small dialog whenever the app is opened without permission
            if (!dialogShownThisResume) {
                dialogShownThisResume = true
                showUsageAccessDialog()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(refresher)
    }

    // ---- Permission dialog + settings deep-link ----

    private fun showUsageAccessDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Allow Usage Access?")
            .setMessage("LatencyChecker needs Usage Access to show per-app Wi-Fi/Mobile usage. This stays on your device.")
            .setNegativeButton("Don’t allow", null)
            .setPositiveButton("Allow") { _, _ -> openUsageAccessSettings() }
            .setCancelable(true)
            .show()
    }

    private fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            // Some OEMs honor these hints; harmless elsewhere
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            data = Uri.fromParts("package", packageName, null)
        }
        try {
            usageAccessLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            usageAccessLauncher.launch(details)
        }
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    // ---- Data + UI ----

    private fun startRefreshing() {
        loadAndRender()
        handler.postDelayed(refresher, 30_000)
    }

    private fun loadAndRender() {
        val end = System.currentTimeMillis()
        val start = max(0L, end - timeframeMillis)

        binding.swipeRefresh.isRefreshing = true
        CoroutineScope(Dispatchers.IO).launch {
            val data = UsageStatsHelper.getAppDataUsage(this@UsageStatsActivity, start, end)
            withContext(Dispatchers.Main) {
                fullList = data
                applyFiltersAndSort()
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun applyFiltersAndSort() {
        val q = binding.searchInput.text?.toString()?.trim()?.lowercase().orEmpty()
        var list = if (q.isEmpty()) fullList else fullList.filter {
            it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
        list = when (sortBy) {
            SortBy.TOTAL  -> list.sortedByDescending { it.totalBytes }
            SortBy.WIFI   -> list.sortedByDescending { it.wifiBytes }
            SortBy.MOBILE -> list.sortedByDescending { it.mobileBytes }
            SortBy.NAME   -> list.sortedBy { it.appName.lowercase() }
        }
        adapter.submit(list)
    }
}
