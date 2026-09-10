package com.cafebunchai.pos.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cafebunchai.pos.data.model.InventoryItem
import com.cafebunchai.pos.ui.util.qtyLabel

object StockAlerts {
    const val CHANNEL_ID = "stock_refill"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Stock refill",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "When kitchen stock is about to run out"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun notifyRefill(context: Context, items: List<InventoryItem>) {
        if (items.isEmpty()) return
        createChannel(context)
        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return
        val title = if (items.size == 1) "Refill ${items.first().name}" else "Stock running low"
        val body = items.joinToString { "${it.name} (${qtyLabel(it.qtyOnHand, it.unit)})" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText("Refill: $body")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Refill needed: $body"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .build()
        val id = ((System.currentTimeMillis() and 0x7fffffff).toInt()).coerceAtLeast(1)
        runCatching { nm.notify(id, notification) }
    }
}
