package fm.mrc.todolist.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val todoTitle = intent.getStringExtra("todoTitle") ?: return
        val todoId = intent.getIntExtra("todoId", 0)
        
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showNotification(todoTitle, todoId)
    }
} 