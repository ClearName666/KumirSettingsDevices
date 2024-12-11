package com.kumir.settingupdevices

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        // Создаем канал уведомлений для Android 8.0 и выше
        createNotificationChannel(applicationContext)

        // Отправляем уведомление
        sendNotification(applicationContext, "Фоновое уведомление", "Уведомление отправлено в фоновом режиме")

        return Result.success()
    }

    // для создания канала уведомлений
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "your_channel_id"
            val channelName = "Your Channel Name"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Your channel description"
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // функция для отправки уведомления
    private fun sendNotification(context: Context, title: String, content: String) {
        val channelId = "your_channel_id"
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // отправка уведомления
        with(NotificationManagerCompat.from(context)) {
            notify(1, builder.build())
        }
    }
}
