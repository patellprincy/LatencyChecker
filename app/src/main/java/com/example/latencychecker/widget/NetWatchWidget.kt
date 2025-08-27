package com.example.latencychecker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.latencychecker.R
import com.example.latencychecker.net.SpeedTestRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetWatchWidget : AppWidgetProvider() {
    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val rv = RemoteViews(ctx.packageName, R.layout.widget_netwatch)
            val intent = Intent(ctx, javaClass).apply { action = "REFRESH" }
            val pi = PendingIntent.getBroadcast(ctx, id, intent, PendingIntent.FLAG_IMMUTABLE)
            rv.setOnClickPendingIntent(R.id.speedText, pi)
            mgr.updateAppWidget(id, rv)
        }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        if (intent.action == "REFRESH") {
            CoroutineScope(Dispatchers.IO).launch {
                val ping = SpeedTestRunner.runPing()
                val dl = SpeedTestRunner.runDownload()
                val mgr = AppWidgetManager.getInstance(ctx)
                val ids = mgr.getAppWidgetIds(ComponentName(ctx, javaClass))
                val rv = RemoteViews(ctx.packageName, R.layout.widget_netwatch)
                rv.setTextViewText(R.id.speedText, String.format("%.1f Mbps", dl))
                rv.setTextViewText(R.id.latText, String.format("%d ms", ping))
                ids.forEach { mgr.updateAppWidget(it, rv) }
            }
        }
    }
}
