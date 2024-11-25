package fm.mrc.todolist.data

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import fm.mrc.todolist.data.gson.LocalDateAdapter
import java.time.LocalDate
import java.util.UUID

class UserManager(private val context: Context) {
    private var currentUser: User? = null
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .create()
    private val sharedPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

    init {
        checkRememberedUser()
    }

    private fun checkRememberedUser() {
        val rememberedEmail = sharedPrefs.getString("rememberedEmail", null)
        val rememberedPassword = sharedPrefs.getString("rememberedPassword", null)
        
        if (rememberedEmail != null && rememberedPassword != null) {
            login(rememberedEmail, rememberedPassword, true)
        }
    }

    private fun loadUsers(): Map<String, User> {
        val json = sharedPrefs.getString("users", "{}")
        val type = object : TypeToken<Map<String, User>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    private fun saveUsers(users: Map<String, User>) {
        sharedPrefs.edit()
            .putString("users", gson.toJson(users))
            .apply()
    }

    fun createUser(email: String, password: String, rememberMe: Boolean = false): Boolean {
        if (email.isBlank() || password.isBlank() || !email.contains("@")) {
            return false
        }

        val users = loadUsers().toMutableMap()
        if (users.containsKey(email)) {
            return false
        }

        val newUser = User(
            id = UUID.randomUUID().toString(),
            email = email,
            password = password
        )
        users[email] = newUser
        saveUsers(users)
        currentUser = newUser
        
        if (rememberMe) {
            rememberUser(email, password)
        }
        
        return true
    }

    fun login(email: String, password: String, rememberMe: Boolean = false): Boolean {
        val users = loadUsers()
        val user = users[email]
        return if (user != null && user.password == password) {
            currentUser = user
            if (rememberMe) {
                rememberUser(email, password)
            }
            true
        } else {
            false
        }
    }

    private fun rememberUser(email: String, password: String) {
        sharedPrefs.edit()
            .putString("rememberedEmail", email)
            .putString("rememberedPassword", password)
            .apply()
    }

    fun logout() {
        currentUser = null
        sharedPrefs.edit()
            .remove("rememberedEmail")
            .remove("rememberedPassword")
            .apply()
    }

    fun getCurrentUser(): User? = currentUser

    fun saveUserTodos(todos: List<TodoItem>) {
        currentUser?.let { user ->
            val users = loadUsers().toMutableMap()
            users[user.email] = user.copy(todos = todos)
            saveUsers(users)
            currentUser = users[user.email]
        }
    }
} 