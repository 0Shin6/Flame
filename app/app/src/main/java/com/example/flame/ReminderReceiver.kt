package com.example.flame

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra("TYPE") ?: "RAPPEL"
        val titre = intent.getStringExtra("TITRE") ?: "Tâche"
        val desc = intent.getStringExtra("DESC") ?: ""
        val id = intent.getLongExtra("ID", System.currentTimeMillis())

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "flame_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifications Flame",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationTitle = if (type == "RETARD") "🚨 Tâche en retard !" else "🔔 Rappel : $titre"
        val notificationText = if (type == "RETARD") "La tâche \"$titre\" est arrivée à échéance." else desc

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // Utiliser des IDs différents pour ne pas écraser les notifications
        val finalId = if (type == "RETARD") (id + 1).toInt() else id.toInt()
        notificationManager.notify(finalId, notification)
    }
}
