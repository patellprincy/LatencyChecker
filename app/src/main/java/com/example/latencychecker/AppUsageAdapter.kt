package com.example.latencychecker

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppUsageAdapter : RecyclerView.Adapter<AppUsageAdapter.UsageViewHolder>() {

    private var dataList = listOf<AppDataUsage>()
    private var totalDataBytes: Long = 0

    fun submit(data: List<AppDataUsage>) {
        dataList = data
        totalDataBytes = data.sumOf { it.totalBytes } // Calculate total once
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_app_usage_adapter, parent, false)
        return UsageViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsageViewHolder, position: Int) {
        val item = dataList[position]

        holder.icon.setImageDrawable(item.appIcon)
        holder.label.text = item.appName
        holder.packageName.text = item.packageName

        // Calculate percentage
        val percentage = if (totalDataBytes > 0) {
            (item.totalBytes.toDouble() / totalDataBytes.toDouble()) * 100
        } else 0.0

        // Show formatted size + percentage
        holder.bytes.text = "Total: ${Formatter.formatFileSize(holder.itemView.context, item.totalBytes)} (${String.format("%.1f", percentage)}%)"

        // Progress bar shows proportion of total usage
        holder.progress.progress = percentage.toInt()
    }

    override fun getItemCount(): Int = dataList.size

    class UsageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val label: TextView = view.findViewById(R.id.label)
        val packageName: TextView = view.findViewById(R.id.packageName)
        val bytes: TextView = view.findViewById(R.id.bytes)
        val progress: ProgressBar = view.findViewById(R.id.progress)
    }
}
