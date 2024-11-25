package fm.mrc.todolist

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Logger

class TodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            FirebaseApp.initializeApp(this)
            
            // Initialize Firebase Database with persistence and correct URL
            val database = FirebaseDatabase.getInstance("https://todolist-f954c-default-rtdb.asia-southeast1.firebasedatabase.app")
            database.setPersistenceEnabled(true)
            
            // Enable logging for debug purposes
            database.setLogLevel(Logger.Level.DEBUG)
            
            // Initialize Auth
            FirebaseAuth.getInstance()
            
        } catch (e: Exception) {
            Log.e("TodoApplication", "Error initializing Firebase", e)
        }
    }
} 