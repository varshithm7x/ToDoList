package fm.mrc.todolist.data

data class User(
    val id: String,
    val email: String,
    val password: String? = null,
    val todos: List<TodoItem> = emptyList()
) {
    fun copy(todos: List<TodoItem> = this.todos): User {
        return User(id, email, password, todos)
    }
} 