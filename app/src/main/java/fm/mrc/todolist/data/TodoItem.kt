package fm.mrc.todolist.data

import java.time.LocalDate

data class TodoItem(
    val id: Int,
    val title: String,
    val isCompleted: Boolean = false,
    val date: LocalDate? = null,
    val timeSlot: TimeSlot? = null
) 