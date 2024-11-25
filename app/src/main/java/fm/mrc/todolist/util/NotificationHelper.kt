package fm.mrc.todolist.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import fm.mrc.todolist.MainActivity
import fm.mrc.todolist.R
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import android.app.AlarmManager
import fm.mrc.todolist.data.TodoItem
import fm.mrc.todolist.data.TimeSlot

class NotificationHelper(private val context: Context) {
    private val channelId = "todo_reminder_channel"
    
    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Todo Reminders"
            val descriptionText = "Notifications for todo reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleNotification(todoItem: TodoItem) {
        if (!hasNotificationPermission()) {
            return
        }

        val notificationTime = calculateNotificationTime(todoItem.date, todoItem.timeSlot)
        if (notificationTime != null) {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("todoTitle", todoItem.title)
                putExtra("todoId", todoItem.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                todoItem.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                notificationTime,
                pendingIntent
            )
        }
    }

    fun cancelNotification(todoId: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            todoId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }

    private fun calculateNotificationTime(date: LocalDate?, timeSlot: TimeSlot?): Long? {
        if (date == null) return null
        
        val time = if (timeSlot != null) {
            // Parse the time slot's start time
            LocalTime.parse(timeSlot.startTime)
        } else {
            // Default to 9:00 AM if no time specified
            LocalTime.of(9, 0)
        }

        return date.atTime(time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    fun showNotification(title: String, id: Int) {
        if (!hasNotificationPermission()) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Todo Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
} 