package fm.mrc.todolist.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import kotlinx.coroutines.delay

class ServerUserManager(private val context: Context) {
    private val auth: FirebaseAuth by lazy { 
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e("ServerUserManager", "Error initializing Firebase Auth", e)
            throw e
        }
    }
    
    private val database by lazy {
        try {
            FirebaseDatabase.getInstance("https://todolist-f954c-default-rtdb.asia-southeast1.firebasedatabase.app").reference
        } catch (e: Exception) {
            Log.e("ServerUserManager", "Error initializing Firebase Database", e)
            throw e
        }
    }
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    private var todosListener: ValueEventListener? = null

    init {
        try {
            auth.currentUser?.let { firebaseUser ->
                attachTodosListener(firebaseUser.uid)
            }
        } catch (e: Exception) {
            Log.e("ServerUserManager", "Error in initialization", e)
        }
    }

    private fun attachTodosListener(userId: String) {
        try {
            Log.d("ServerUserManager", "Attaching todos listener for user: $userId")
            
            // Remove any existing listener
            todosListener?.let { oldListener ->
                database.child("users").child(userId).child("todos").removeEventListener(oldListener)
            }

            // Create and attach new listener
            todosListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("ServerUserManager", "Data change detected. Snapshot exists: ${snapshot.exists()}")
                    Log.d("ServerUserManager", "Snapshot value: ${snapshot.value}")
                    
                    try {
                        val todosList = if (snapshot.exists()) {
                            snapshot.children.mapNotNull { todoSnapshot ->
                                Log.d("ServerUserManager", "Processing todo: ${todoSnapshot.value}")
                                
                                val todoMap = todoSnapshot.value as? Map<*, *>
                                if (todoMap == null) {
                                    Log.e("ServerUserManager", "Failed to cast todo to Map")
                                    return@mapNotNull null
                                }

                                try {
                                    TodoItem(
                                        id = (todoMap["id"] as? Number)?.toInt() ?: run {
                                            Log.e("ServerUserManager", "Invalid or missing id")
                                            return@mapNotNull null
                                        },
                                        title = todoMap["title"] as? String ?: run {
                                            Log.e("ServerUserManager", "Invalid or missing title")
                                            return@mapNotNull null
                                        },
                                        isCompleted = todoMap["isCompleted"] as? Boolean ?: false,
                                        date = (todoMap["date"] as? String)?.let { dateStr -> 
                                            try {
                                                LocalDate.parse(dateStr)
                                            } catch (e: Exception) {
                                                Log.e("ServerUserManager", "Error parsing date: $dateStr", e)
                                                null
                                            }
                                        },
                                        timeSlot = (todoMap["timeSlot"] as? Map<*, *>)?.let { timeSlotMap ->
                                            TimeSlot(
                                                id = (timeSlotMap["id"] as? Number)?.toInt() ?: return@let null,
                                                startTime = timeSlotMap["startTime"] as? String ?: return@let null,
                                                endTime = timeSlotMap["endTime"] as? String ?: return@let null,
                                                displayName = timeSlotMap["displayName"] as? String ?: return@let null
                                            )
                                        }
                                    )
                                } catch (e: Exception) {
                                    Log.e("ServerUserManager", "Error creating TodoItem", e)
                                    null
                                }
                            }.toList()
                        } else {
                            emptyList()
                        }

                        Log.d("ServerUserManager", "Parsed ${todosList.size} todos")
                        
                        val currentUserValue = _currentUser.value
                        Log.d("ServerUserManager", "Current user before update: $currentUserValue")
                        
                        _currentUser.value = currentUserValue?.copy(todos = todosList)
                        
                        Log.d("ServerUserManager", "Current user after update: ${_currentUser.value}")
                    } catch (e: Exception) {
                        Log.e("ServerUserManager", "Error in onDataChange", e)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ServerUserManager", "Database error: ${error.message}")
                }
            }

            // Attach the listener
            val todosRef = database.child("users").child(userId).child("todos")
            Log.d("ServerUserManager", "Attaching listener to path: ${todosRef.path}")
            todosRef.addValueEventListener(todosListener!!)
            
        } catch (e: Exception) {
            Log.e("ServerUserManager", "Error attaching todos listener", e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<User> {
        return try {
            Log.d("ServerUserManager", "Attempting signup for email: $email")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Failed to create user")
            
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                todos = emptyList()
            )
            
            // Initialize with empty todos map
            database.child("users")
                .child(firebaseUser.uid)
                .child("todos")
                .setValue(mapOf<String, Any>())
                .await()
            
            attachTodosListener(firebaseUser.uid)
            _currentUser.value = user
            Log.d("ServerUserManager", "Signup successful for uid: ${user.id}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e("ServerUserManager", "Error during signup", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Failed to sign in")
            
            attachTodosListener(firebaseUser.uid)
            
            val user = User(
                id = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                todos = emptyList()  // Will be populated by the listener
            )
            
            _currentUser.value = user
            Result.success(user)
        } catch (e: Exception) {
            Log.e("ServerUserManager", "Error during sign in", e)
            Result.failure(e)
        }
    }

    suspend fun saveUserTodos(todos: List<TodoItem>) {
        try {
            val firebaseUser = auth.currentUser ?: run {
                Log.e("ServerUserManager", "Cannot save todos: No authenticated user")
                return
            }

            Log.d("ServerUserManager", "Starting to save ${todos.size} todos for user ${firebaseUser.uid}")
            
            // Convert TodoItems to a list instead of a map
            val todosList = todos.map { todo ->
                mapOf(
                    "id" to todo.id,
                    "title" to todo.title,
                    "isCompleted" to todo.isCompleted,
                    "date" to todo.date?.toString(),
                    "timeSlot" to todo.timeSlot?.let { timeSlot ->
                        mapOf(
                            "id" to timeSlot.id,
                            "startTime" to timeSlot.startTime,
                            "endTime" to timeSlot.endTime,
                            "displayName" to timeSlot.displayName
                        )
                    }
                )
            }

            Log.d("ServerUserManager", "Converted todos to list: $todosList")

            // Save to Firebase with await()
            val todosRef = database.child("users").child(firebaseUser.uid).child("todos")
            Log.d("ServerUserManager", "Saving to path: ${todosRef.path}")
            
            todosRef.setValue(todosList).await()
            Log.d("ServerUserManager", "Successfully saved todos to Firebase")
            
            // Update current user with new todos
            _currentUser.value = _currentUser.value?.copy(todos = todos)
            
            // Add this right after setValue()
            database.child("users").child(firebaseUser.uid).child("todos")
                .get()
                .addOnSuccessListener { snapshot ->
                    Log.d("ServerUserManager", "Verification - Read saved data: ${snapshot.value}")
                }
                .addOnFailureListener { e ->
                    Log.e("ServerUserManager", "Verification - Failed to read saved data", e)
                }
            
        } catch (e: Exception) {
            Log.e("ServerUserManager", "Error in saveUserTodos", e)
            when (e) {
                is CancellationException -> throw e
                else -> throw e
            }
        }
    }

    fun logout() {
        try {
            // Remove the todos listener
            auth.currentUser?.let { firebaseUser ->
                todosListener?.let { listener ->
                    database.child("users").child(firebaseUser.uid).child("todos")
                        .removeEventListener(listener)
                }
                todosListener = null
            }
            
            auth.signOut()
            _currentUser.value = null
            Log.d("ServerUserManager", "User logged out and listeners cleaned up")
        } catch (e: Exception) {
            Log.e("ServerUserManager", "Error during logout", e)
        }
    }
} 