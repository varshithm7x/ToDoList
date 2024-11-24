package fm.mrc.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import fm.mrc.todolist.ui.theme.ToDoListTheme
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import java.util.Calendar
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.ArrowBack
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.animate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.delay
import android.app.TimePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextAlign

data class TodoItem(
    val id: Int,
    var title: String,
    var isCompleted: Boolean = false,
    val date: LocalDate? = null,
    val timeSlot: TimeSlot? = null
)

data class TimeSlot(
    val id: Int,
    val startTime: String,
    val endTime: String,
    val displayName: String
)

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

class LocalDateAdapter : TypeAdapter<LocalDate>() {
    override fun write(out: JsonWriter, value: LocalDate?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value.toString())
        }
    }

    override fun read(input: JsonReader): LocalDate? {
        val dateStr = input.nextString()
        return if (dateStr == "null") null else LocalDate.parse(dateStr)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToDoListTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TodoScreen(context = this)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(context: Context) {
    var showTutorial by remember {
        mutableStateOf(
            context.getSharedPreferences("TodoPrefs", Context.MODE_PRIVATE)
                .getBoolean("firstLaunch", true)
        )
    }

    if (showTutorial) {
        TutorialDialog(
            onDismiss = {
                showTutorial = false
                context.getSharedPreferences("TodoPrefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("firstLaunch", false)
                    .apply()
            }
        )
    }

    var todoItems by remember { 
        mutableStateOf(loadTodoItems(context))
    }
    var newTodoTitle by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isCalendarView by remember { mutableStateOf(false) }
    var isTimetableView by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showTypeDialog by remember { mutableStateOf(false) }
    
    // Track expanded states for each day
    val expandedStates = remember {
        mutableStateMapOf<LocalDate, Boolean>()
    }

    // Save items whenever the list changes
    LaunchedEffect(todoItems) {
        saveTodoItems(context, todoItems)
    }

    if (showDatePicker) {
        CustomDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { 
                selectedDate = it
                showDatePicker = false 
            }
        )
    }

    // Handle back button press
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(backDispatcher) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    showMenu -> {
                        showMenu = false
                        isCalendarView = false
                        isTimetableView = false
                    }
                    showTypeDialog -> {
                        showTypeDialog = false
                    }
                }
            }
        }
        backDispatcher?.addCallback(callback)
        onDispose {
            callback.remove()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showMenu) {
                CenterAlignedTopAppBar(
                    title = { Text("Todo List") },
                    navigationIcon = {
                        IconButton(onClick = {
                            showMenu = false
                            isCalendarView = false
                            isTimetableView = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to home"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            if (!showMenu) {
                FloatingActionButton(
                    onClick = { showTypeDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Todo"
                    )
                }
            }
        }
    ) { padding ->
        if (showTypeDialog) {
            AlertDialog(
                onDismissRequest = { showTypeDialog = false },
                title = { Text("Select Todo Type") },
                text = {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FilledTonalButton(
                            onClick = {
                                isCalendarView = false
                                isTimetableView = false
                                showMenu = true
                                showTypeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.List,
                                    contentDescription = "List View",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Simple List")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        FilledTonalButton(
                            onClick = {
                                isCalendarView = true
                                isTimetableView = false
                                showMenu = true
                                showTypeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Calendar View",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Calendar Based")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        FilledTonalButton(
                            onClick = {
                                isTimetableView = true
                                isCalendarView = false
                                showMenu = true
                                showTypeDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = "Time Based",
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Time Based")
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showTypeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showMenu) {
            // Show the full todo interface when menu is active
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Input Section
                if (isTimetableView) {
                    TimetableInputSection(
                        newTodoTitle = newTodoTitle,
                        onTitleChange = { newTodoTitle = it },
                        onAddTodo = { date, timeSlot ->
                            if (newTodoTitle.isNotBlank()) {
                                todoItems = todoItems + TodoItem(
                                    id = todoItems.size,
                                    title = newTodoTitle,
                                    date = date,
                                    timeSlot = timeSlot
                                )
                                newTodoTitle = ""
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // Add Todo Input with Date
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTodoTitle,
                                onValueChange = { newTodoTitle = it },
                                modifier = if (isCalendarView) Modifier.weight(1f) else Modifier.fillMaxWidth(),
                                placeholder = { Text("Enter todo item") },
                                trailingIcon = {
                                    Button(
                                        onClick = {
                                            if (newTodoTitle.isNotBlank()) {
                                                todoItems = todoItems + TodoItem(
                                                    id = todoItems.size,
                                                    title = newTodoTitle,
                                                    date = if (isCalendarView) selectedDate else null
                                                )
                                                newTodoTitle = ""
                                                selectedDate = null
                                            }
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text("Add")
                                    }
                                }
                            )
                            if (isCalendarView) {
                                Spacer(modifier = Modifier.width(8.dp))
                                FilledTonalButton(
                                    onClick = { showDatePicker = true }
                                ) {
                                    Text(
                                        text = selectedDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) 
                                            ?: "Set Date"
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display Section
                if (isTimetableView) {
                    TimetableView(
                        todoItems = todoItems.filter { it.timeSlot != null },
                        onCheckedChange = { item, checked ->
                            todoItems = todoItems.map { 
                                if (it.id == item.id) it.copy(isCompleted = checked)
                                else it
                            }
                        },
                        onDelete = { item ->
                            todoItems = todoItems.filter { it.id != item.id }
                        }
                    )
                } else {
                    // Todo List grouped by date
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val groupedItems = if (isCalendarView) {
                            // Filter out items without dates and sort by date
                            todoItems
                                .filter { it.date != null }
                                .sortedBy { it.date }
                                .groupBy { it.date }
                        } else {
                            // Original grouping logic
                            todoItems.groupBy { it.date }
                        }
                        
                        if (isCalendarView) {
                            // Show only items with dates in chronological order
                            groupedItems.entries.sortedBy { it.key }.forEach { (date, items) ->
                                item {
                                    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
                                    Text(
                                        text = date?.format(dateFormatter) ?: "No Date",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                
                                items(items) { item ->
                                    TodoItemRow(
                                        item = item,
                                        onCheckedChange = { checked ->
                                            todoItems = todoItems.map { 
                                                if (it.id == item.id) it.copy(isCompleted = checked)
                                                else it
                                            }
                                        },
                                        onDelete = {
                                            todoItems = todoItems.filter { it.id != item.id }
                                        }
                                    )
                                }
                            }
                        } else {
                            // Original display logic for non-calendar view
                            groupedItems.forEach { (date, items) ->
                                item {
                                    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
                                    Text(
                                        text = date?.format(dateFormatter) ?: "No Date",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                
                                items(items) { item ->
                                    TodoItemRow(
                                        item = item,
                                        onCheckedChange = { checked ->
                                            todoItems = todoItems.map { 
                                                if (it.id == item.id) it.copy(isCompleted = checked)
                                                else it
                                            }
                                        },
                                        onDelete = {
                                            todoItems = todoItems.filter { it.id != item.id }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Empty homepage with just the FAB
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                var displayedText by remember { mutableStateOf("") }
                val fullText = "Welcome to Todo List"
                var textAlpha by remember { mutableStateOf(0f) }
                
                LaunchedEffect(Unit) {
                    // Fade in animation
                    animate(0f, 1f) { value, _ ->
                        textAlpha = value
                    }
                    // Typewriter animation
                    for (i in fullText.indices) {
                        delay(100) // Adjust this value to control typing speed
                        displayedText = fullText.substring(0, i + 1)
                    }
                }
                
                Text(
                    text = displayedText,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer(alpha = textAlpha)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoItemRow(
    item: TodoItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = onCheckedChange
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = if (item.isCompleted) {
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        MaterialTheme.typography.bodyLarge
                    }
                )
                item.date?.let { date ->
                    Text(
                        text = date.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete todo"
                )
            }
        }
    }
}

private fun saveTodoItems(context: Context, items: List<TodoItem>) {
    val sharedPrefs = context.getSharedPreferences("TodoPrefs", Context.MODE_PRIVATE)
    val gson = Gson().newBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .create()
    val json = gson.toJson(items)
    sharedPrefs.edit().putString("todos", json).apply()
}

private fun loadTodoItems(context: Context): List<TodoItem> {
    val sharedPrefs = context.getSharedPreferences("TodoPrefs", Context.MODE_PRIVATE)
    val gson = Gson().newBuilder()
        .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
        .create()
    val json = sharedPrefs.getString("todos", "[]")
    val type = object : TypeToken<List<TodoItem>>() {}.type
    return gson.fromJson(json, type)
}

@Composable
fun CustomDatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                onDismissRequest()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
        
        onDispose {
            datePickerDialog.dismiss()
        }
    }
}

@Composable
fun TimetableInputSection(
    newTodoTitle: String,
    onTitleChange: (String) -> Unit,
    onAddTodo: (LocalDate, TimeSlot) -> Unit
) {
    var selectedTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showAddTimeSlotDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = newTodoTitle,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter schedule item") }
        )
        
        // Date Selection Button
        FilledTonalButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedDate?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) ?: "Select Date")
        }
        
        // Add Time Slot Button
        FilledTonalButton(
            onClick = { showAddTimeSlotDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add New Time Slot")
        }
        
        // Time Slot Selection - Show only available time slots
        TimeSlotManager.getTimeSlots().forEach { timeSlot ->
            OutlinedButton(
                onClick = { 
                    selectedTimeSlot = timeSlot
                    selectedDate?.let { date ->
                        if (newTodoTitle.isNotBlank()) {
                            onAddTodo(date, timeSlot)
                            onTitleChange("")
                            selectedDate = null
                            selectedTimeSlot = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selectedTimeSlot == timeSlot) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Text("${timeSlot.displayName} (${timeSlot.startTime} - ${timeSlot.endTime})")
            }
        }
    }

    if (showDatePicker) {
        CustomDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            onDateSelected = { 
                selectedDate = it
                showDatePicker = false
            }
        )
    }

    if (showAddTimeSlotDialog) {
        AddTimeSlotDialog(
            onDismiss = { showAddTimeSlotDialog = false },
            onTimeSlotAdded = { startTime, endTime, displayName ->
                TimeSlotManager.addTimeSlot(startTime, endTime, displayName)
                showAddTimeSlotDialog = false
            }
        )
    }
}

@Composable
fun TimetableView(
    todoItems: List<TodoItem>,
    onCheckedChange: (TodoItem, Boolean) -> Unit,
    onDelete: (TodoItem) -> Unit
) {
    val today = LocalDate.now()
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    
    var expandedDates by remember { mutableStateOf(setOf<LocalDate?>()) }
    
    // Completely reworked processing logic
    val processedItems = remember(todoItems) {
        todoItems
            .filter { it.date != null && it.timeSlot != null }
            .distinctBy { it.id } // Remove any duplicate items
            .groupBy { it.date }
            .mapValues { (_, items) -> 
                items.distinctBy { Triple(it.id, it.title, it.timeSlot?.id) }
            }
            .toList()
            .sortedBy { (date, _) -> date }
            .distinctBy { (date, _) -> date } // Ensure only one entry per date
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(processedItems) { (date, items) ->
            val isExpanded = date == today || expandedDates.contains(date)
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (date == today) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = date != today) {
                                expandedDates = if (isExpanded) {
                                    expandedDates - date
                                } else {
                                    expandedDates + date
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (date == today) "Today" else date?.format(dateFormatter) ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        if (date != today) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val timeSlotGroups = items.groupBy { it.timeSlot }
                        timeSlotGroups.forEach { (timeSlot, timeSlotItems) ->
                            if (timeSlot != null) {
                                Text(
                                    text = "${timeSlot.displayName} (${timeSlot.startTime} - ${timeSlot.endTime})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                timeSlotItems.forEach { item ->
                                    TodoItemRow(
                                        item = item,
                                        onCheckedChange = { checked -> onCheckedChange(item, checked) },
                                        onDelete = { onDelete(item) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddTimeSlotDialog(
    onDismiss: () -> Unit,
    onTimeSlotAdded: (String, String, String) -> Unit
) {
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Time picker dialogs
    if (showStartTimePicker) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                startTime = String.format("%02d:%02d", hourOfDay, minute)
                showStartTimePicker = false
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24-hour format
        )
        DisposableEffect(Unit) {
            timePickerDialog.show()
            onDispose {
                timePickerDialog.dismiss()
            }
        }
    }

    if (showEndTimePicker) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                endTime = String.format("%02d:%02d", hourOfDay, minute)
                showEndTimePicker = false
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24-hour format
        )
        DisposableEffect(Unit) {
            timePickerDialog.show()
            onDispose {
                timePickerDialog.dismiss()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Time Slot") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Name (e.g., 'Lunch Break')") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Start Time Button
                OutlinedButton(
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = "Start Time")
                        Text(if (startTime.isEmpty()) "Select Start Time" else "Start Time: $startTime")
                    }
                }
                
                // End Time Button
                OutlinedButton(
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = "End Time")
                        Text(if (endTime.isEmpty()) "Select End Time" else "End Time: $endTime")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (startTime.isNotBlank() && endTime.isNotBlank() && displayName.isNotBlank()) {
                        onTimeSlotAdded(startTime, endTime, displayName)
                    }
                },
                enabled = startTime.isNotBlank() && endTime.isNotBlank() && displayName.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TutorialDialog(
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 4

    val tutorialSteps = listOf(
        Triple(
            "Welcome to Todo List!",
            "This app helps you organize your tasks in three different ways.",
            Icons.Default.List
        ),
        Triple(
            "Simple List",
            "Create basic todo items without dates or times. Perfect for quick tasks and shopping lists.",
            Icons.Default.List
        ),
        Triple(
            "Calendar Based",
            "Organize your tasks by date. Great for planning ahead and managing deadlines.",
            Icons.Default.CalendarMonth
        ),
        Triple(
            "Time Based",
            "Schedule tasks in specific time slots. Ideal for daily routines and appointments.",
            Icons.Default.Schedule
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tutorialSteps[currentStep].first,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = tutorialSteps[currentStep].third,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = tutorialSteps[currentStep].second,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                LinearProgressIndicator(
                    progress = (currentStep + 1).toFloat() / totalSteps,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (currentStep < totalSteps - 1) {
                        currentStep++
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (currentStep < totalSteps - 1) "Next" else "Get Started")
            }
        },
        dismissButton = if (currentStep > 0) {
            {
                TextButton(onClick = { currentStep-- }) {
                    Text("Previous")
                }
            }
        } else null
    )
}