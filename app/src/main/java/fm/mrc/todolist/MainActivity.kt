package fm.mrc.todolist

import android.os.Bundle
import android.os.Build
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
import fm.mrc.todolist.ui.theme.ToDoListTheme
import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import java.util.Calendar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.animate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.delay
import android.app.TimePickerDialog
import androidx.compose.ui.text.style.TextAlign
import fm.mrc.todolist.data.TodoItem
import fm.mrc.todolist.data.TimeSlot
import fm.mrc.todolist.data.TimeSlotManager
import fm.mrc.todolist.data.ServerUserManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import fm.mrc.todolist.ui.LoginScreen
import fm.mrc.todolist.ui.components.LogoutButton
import fm.mrc.todolist.ui.components.LogoutConfirmationDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import fm.mrc.todolist.util.NotificationHelper
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var userManager: ServerUserManager
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userManager = ServerUserManager(this)
        notificationHelper = NotificationHelper(this)
        
        enableEdgeToEdge()
        setContent {
            ToDoListTheme(userPreferredTheme = "light") {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUser by userManager.currentUser.collectAsStateWithLifecycle()
                    
                    if (currentUser != null) {
                        MainContent(
                            modifier = Modifier.fillMaxSize(),
                            userManager = userManager
                        )
                    } else {
                        LoginScreen(
                            onLoginSuccess = { /* No need to update isLoggedIn here */ },
                            userManager = userManager
                        )
                    }
                }
            }
        }

        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainContent(
        modifier: Modifier = Modifier,
        userManager: ServerUserManager
    ) {
        var showLogoutDialog by remember { mutableStateOf(false) }

        if (showLogoutDialog) {
            LogoutConfirmationDialog(
                onConfirm = {
                    userManager.logout()
                    showLogoutDialog = false
                },
                onDismiss = {
                    showLogoutDialog = false
                }
            )
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Todo List") },
                    actions = {
                        LogoutButton(
                            onLogout = { showLogoutDialog = true }
                        )
                    }
                )
            }
        ) { paddingValues ->
            TodoScreen(
                modifier = Modifier.padding(paddingValues),
                context = LocalContext.current,
                userManager = userManager,
                onLogout = { showLogoutDialog = true },
                notificationHelper = notificationHelper
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    modifier: Modifier = Modifier,
    context: Context,
    userManager: ServerUserManager,
    onLogout: () -> Unit,
    notificationHelper: NotificationHelper
) {
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

    val scope = rememberCoroutineScope()
    val currentUser by userManager.currentUser.collectAsStateWithLifecycle()
    var todoItems by remember { 
        mutableStateOf(currentUser?.todos ?: emptyList())
    }

    // Update todoItems whenever currentUser changes
    LaunchedEffect(currentUser) {
        todoItems = currentUser?.todos ?: emptyList()
    }

    // Save items whenever the list changes
    LaunchedEffect(todoItems) {
        if (currentUser != null) {
            try {
                scope.launch {
                    delay(500) // Debounce delay
                    userManager.saveUserTodos(todoItems)
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Log.e("TodoScreen", "Error saving todos", e)
                }
            }
        }
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
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back to home",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onLogout) {
                            Icon(
                                imageVector = Icons.Filled.ExitToApp,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                // Add a TopAppBar for the main screen as well
                CenterAlignedTopAppBar(
                    title = { Text("Todo List") },
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
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Todo"
                    )
                }
            }
        }
    ) { innerPadding ->
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
                                    Icons.Filled.List,
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
                                    Icons.Filled.CalendarMonth,
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
                                    Icons.Filled.Schedule,
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
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // Input Section
                if (isTimetableView) {
                    TimetableInputSection(
                        newTodoTitle = newTodoTitle,
                        onTitleChange = { newTodoTitle = it },
                        onAddTodo = { date, timeSlot ->
                            if (newTodoTitle.isNotBlank()) {
                                val newTodo = TodoItem(
                                    id = todoItems.size,
                                    title = newTodoTitle,
                                    date = date,
                                    timeSlot = timeSlot
                                )
                                todoItems = todoItems + newTodo
                                notificationHelper.scheduleNotification(newTodo)
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
                                                val newTodo = TodoItem(
                                                    id = todoItems.size,
                                                    title = newTodoTitle,
                                                    date = if (isCalendarView) selectedDate else null
                                                )
                                                todoItems = todoItems + newTodo
                                                notificationHelper.scheduleNotification(newTodo)
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
                            notificationHelper.cancelNotification(item.id)
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
                                            notificationHelper.cancelNotification(item.id)
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
                                            notificationHelper.cancelNotification(item.id)
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
            // Replace the empty homepage with a conditional check for todos
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                if (todoItems.isEmpty()) {
                    // Show welcome screen for users with no todos
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
                            delay(100)
                            displayedText = fullText.substring(0, i + 1)
                        }
                    }
                    
                    Text(
                        text = displayedText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.graphicsLayer(alpha = textAlpha)
                    )
                } else {
                    // Show existing todos in a simplified view
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(todoItems) { item ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        item.date?.let { date ->
                                            Text(
                                                text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                        item.timeSlot?.let { timeSlot ->
                                            Text(
                                                text = "${timeSlot.startTime} - ${timeSlot.endTime}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                    if (item.isCompleted) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Completed",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
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
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete todo",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
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
                                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
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
                        Icon(Icons.Filled.Schedule, contentDescription = "Start Time")
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
                        Icon(Icons.Filled.Schedule, contentDescription = "End Time")
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
            Icons.Filled.List
        ),
        Triple(
            "Simple List",
            "Create basic todo items without dates or times. Perfect for quick tasks and shopping lists.",
            Icons.Filled.List
        ),
        Triple(
            "Calendar Based",
            "Organize your tasks by date. Great for planning ahead and managing deadlines.",
            Icons.Filled.CalendarMonth
        ),
        Triple(
            "Time Based",
            "Schedule tasks in specific time slots. Ideal for daily routines and appointments.",
            Icons.Filled.Schedule
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