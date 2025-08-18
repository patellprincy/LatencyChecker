package com.example.latencychecker

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AppUsageAdapter : RecyclerView.Adapter<AppUsageAdapter.UsageViewHolder>() {

    private var dataList: List<AppDataUsage> = emptyList()
    private var totalDataBytes: Long = 0L   // sum across the (possibly filtered) list

    fun submit(data: List<AppDataUsage>) {
        dataList = data
        totalDataBytes = data.sumOf { it.totalBytes }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return UsageViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsageViewHolder, position: Int) {
        val item = dataList[position]
        val ctx = holder.itemView.context

        holder.icon.setImageDrawable(item.appIcon)
        holder.label.text = item.appName
        holder.packageName.text = item.packageName

        // Percentage of TOTAL (across list)
        val totalPct = if (totalDataBytes > 0L)
            (item.totalBytes.toDouble() / totalDataBytes.toDouble()) * 100.0 else 0.0

        holder.bytes.text = "Total: ${Formatter.formatFileSize(ctx, item.totalBytes)} " +
                "(${String.format(Locale.US, "%.1f", totalPct)}%)"

        // Per-app composition
        val wifiPctOfApp = if (item.totalBytes > 0L)
            (item.wifiBytes.toDouble() / item.totalBytes.toDouble()) * 100.0 else 0.0
        val mobilePctOfApp = if (item.totalBytes > 0L)
            (item.mobileBytes.toDouble() / item.totalBytes.toDouble()) * 100.0 else 0.0

        if (holder.progress.max != 100) holder.progress.max = 100
        holder.progress.progress = totalPct.toInt().coerceIn(0, 100)

        holder.wifiValue.text = "${Formatter.formatFileSize(ctx, item.wifiBytes)} " +
                "(${String.format(Locale.US, "%.0f", wifiPctOfApp)}%)"
        holder.mobileValue.text = "${Formatter.formatFileSize(ctx, item.mobileBytes)} " +
                "(${String.format(Locale.US, "%.0f", mobilePctOfApp)}%)"

        if (holder.wifiProgress.max != 100) holder.wifiProgress.max = 100
        holder.wifiProgress.progress = wifiPctOfApp.toInt().coerceIn(0, 100)

        if (holder.mobileProgress.max != 100) holder.mobileProgress.max = 100
        holder.mobileProgress.progress = mobilePctOfApp.toInt().coerceIn(0, 100)
    }

    override fun getItemCount(): Int = dataList.size

    class UsageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val label: TextView = view.findViewById(R.id.label)
        val packageName: TextView = view.findViewById(R.id.packageName)
        val bytes: TextView = view.findViewById(R.id.bytes)
        val progress: ProgressBar = view.findViewById(R.id.progress)

        val wifiValue: TextView = view.findViewById(R.id.wifiValue)
        val wifiProgress: ProgressBar = view.findViewById(R.id.wifiProgress)
        val mobileValue: TextView = view.findViewById(R.id.mobileValue)
        val mobileProgress: ProgressBar = view.findViewById(R.id.mobileProgress)
    }
}
