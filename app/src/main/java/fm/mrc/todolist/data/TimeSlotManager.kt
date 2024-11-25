package fm.mrc.todolist.data

class TimeSlotManager {
    companion object {
        private var nextId = 0
        private val timeSlots = mutableListOf<TimeSlot>()

        fun addTimeSlot(startTime: String, endTime: String, displayName: String): TimeSlot {
            val timeSlot = TimeSlot(nextId++, startTime, endTime, displayName)
            timeSlots.add(timeSlot)
            return timeSlot
        }

        fun getTimeSlots(): List<TimeSlot> = timeSlots.toList()

        fun removeTimeSlot(id: Int) {
            timeSlots.removeAll { it.id == id }
        }
    }
} 