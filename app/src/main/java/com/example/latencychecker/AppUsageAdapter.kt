package com.example.latencychecker.adapter

import android.app.usage.UsageStats
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.latencychecker.databinding.ActivityAppUsageAdapterBinding

class AppUsageAdapter(
    private val stats: List<UsageStats>,
    private val pm: PackageManager
) : RecyclerView.Adapter<AppUsageAdapter.UsageViewHolder>() {

    inner class UsageViewHolder(val binding: ActivityAppUsageAdapterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ActivityAppUsageAdapterBinding.inflate(inflater, parent, false)
        return UsageViewHolder(binding)
    }

    override fun getItemCount() = stats.size

    override fun onBindViewHolder(holder: UsageViewHolder, position: Int) {
        val stat = stats[position]
        val appName = try {
            pm.getApplicationLabel(pm.getApplicationInfo(stat.packageName, 0)).toString()
        } catch (e: Exception) {
            stat.packageName
        }
        holder.binding.tvAppName.text = appName
        holder.binding.tvTime.text = "${stat.totalTimeInForeground / 1000} sec"
    }
}
