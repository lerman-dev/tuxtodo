package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.TaskViewModel
import com.example.ui.TaskViewModelFactory
import com.example.ui.theme.TuxTodoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels {
        val database = TaskDatabase.getDatabase(applicationContext)
        val repository = TaskRepository(database.taskDao())
        TaskViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TuxTodoTheme {
                TuxTodoApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuxTodoApp(viewModel: TaskViewModel) {
    // List dialog variables
    var showAddListDialog by remember { mutableStateOf(false) }
    var listToEdit by remember { mutableStateOf<TodoList?>(null) }
    var listToDeleteConfirm by remember { mutableStateOf<TodoList?>(null) }

    // Task dialog variables
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEditByAutosave by remember { mutableStateOf<Task?>(null) }
    var taskToDeleteConfirmByDialog by remember { mutableStateOf<Task?>(null) }

    // State bindings
    val currentListId by viewModel.currentListId.collectAsStateWithLifecycle()
    val allLists by viewModel.allLists.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()

    // Find active list metadata
    val currentSelectedList = remember(currentListId, allLists) {
        allLists.find { it.id == currentListId }
    }

    // Intercept back button to return to home if inside a list
    if (currentListId != null) {
        BackHandler {
            viewModel.currentListId.value = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (currentSelectedList != null) {
                            // Inside a specific list
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentSelectedList.icon,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = currentSelectedList.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            // Dashboard view
                            TuxMiniLogo(modifier = Modifier.size(38.dp))
                            Text(
                                text = stringResource(id = R.string.app_name),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (currentSelectedList != null) {
                        IconButton(onClick = { viewModel.currentListId.value = null }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(id = R.string.back_button)
                            )
                        }
                    }
                },
                actions = {
                    if (currentSelectedList != null) {
                        // Quick Edit List Name inside active list screen
                        IconButton(onClick = { listToEdit = currentSelectedList }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(id = R.string.edit_list)
                            )
                        }
                        IconButton(onClick = { listToDeleteConfirm = currentSelectedList }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete list",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentListId != null) {
                        showAddTaskDialog = true
                    } else {
                        showAddListDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag(if (currentListId != null) "add_task_fab" else "add_list_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(
                        id = if (currentListId != null) R.string.add_task else R.string.add_list
                    ),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (currentListId == null) {
                // SCREEN 1: LIST DASHBOARD SCREEN
                if (allLists.isEmpty()) {
                    TuxEmptyStateView()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Мои списки дел",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                        items(allLists, key = { it.id }) { list ->
                            val listTasks = allTasks.filter { it.listId == list.id }
                            val completedCount = listTasks.count { it.isCompleted }
                            val totalCount = listTasks.size
                            val progressRatio = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.currentListId.value = list.id }
                                    .testTag("list_card_${list.id}"),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = list.icon,
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = list.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (totalCount > 0) {
                                                    "Выполнено: $completedCount из $totalCount"
                                                } else {
                                                    "Список пуст"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(onClick = { listToEdit = list }) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Open list Details",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (totalCount > 0) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            LinearProgressIndicator(
                                                progress = { progressRatio },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(CircleShape),
                                                color = if (progressRatio >= 1.0f) Color(0xFF81C784) else MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // SCREEN 2: TASKS SCREEN INSIDE SELECTED TODO LIST
                // Task filters and Search Input Row
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("search_input"),
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.search_placeholder),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(id = R.string.search_placeholder),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Category scrollable filter chips
                val categories = listOf("Все", "Личное", "Работа", "Учёба", "Linux/Tux")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedCategory.value = category },
                            label = { Text(category) },
                            modifier = Modifier.testTag("chip_category_$category"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Status filter segmented buttons/tabs
                val statuses = listOf("Все", "Активные", "Завершённые")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    statuses.forEach { statusName ->
                        val isSelected = selectedStatus == statusName
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent
                                )
                                .clickable { viewModel.selectedStatus.value = statusName }
                                .padding(vertical = 8.dp)
                                .testTag("tab_status_$statusName"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = statusName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Tasks list view
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (filteredTasks.isEmpty()) {
                        val isSearching = searchQuery.isNotEmpty() || selectedCategory != "Все" || selectedStatus != "Все"
                        if (isSearching) {
                            EmptyStateView(
                                title = stringResource(id = R.string.empty_tasks_search_title),
                                subtitle = stringResource(id = R.string.empty_tasks_search_subtitle),
                                icon = Icons.Outlined.ContentPasteSearch
                            )
                        } else {
                            EmptyStateView(
                                title = "Нет задач в этом списке",
                                subtitle = "Нажмите +, чтобы добавить свою первую задачу с автосохранением!",
                                icon = Icons.Outlined.AssignmentLate
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredTasks, key = { it.id }) { task ->
                                TaskItemCard(
                                    task = task,
                                    onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                    onDelete = { taskToDeleteConfirmByDialog = task },
                                    onEdit = { taskToEditByAutosave = task }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // LIST DIALOGS
    // Create new list dialog
    if (showAddListDialog) {
        ListFormDialog(
            titleString = stringResource(id = R.string.add_list),
            onDismiss = { showAddListDialog = false },
            onConfirm = { name, icon ->
                viewModel.addList(name, icon)
                showAddListDialog = false
            }
        )
    }

    // Edit existing list name / icon dialog
    listToEdit?.let { list ->
        ListFormDialog(
            titleString = stringResource(id = R.string.edit_list),
            initialList = list,
            onDismiss = { listToEdit = null },
            onConfirm = { name, icon ->
                viewModel.updateList(list.copy(name = name, icon = icon))
                listToEdit = null
            }
        )
    }

    // Delete list confirmation
    listToDeleteConfirm?.let { list ->
        AlertDialog(
            onDismissRequest = { listToDeleteConfirm = null },
            title = { Text(text = "Удалить список?") },
            text = { Text(text = stringResource(id = R.string.delete_list_confirmation) + "\n\n${list.icon} ${list.name}") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteList(list)
                        viewModel.currentListId.value = null
                        listToDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(id = R.string.button_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { listToDeleteConfirm = null }) {
                    Text(text = stringResource(id = R.string.button_cancel))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // TASK DIALOGS
    // Create new task dialog
    if (showAddTaskDialog) {
        TaskFormDialog(
            titleString = stringResource(id = R.string.add_task),
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, cat, prio ->
                viewModel.addTask(title, desc, cat, prio)
                showAddTaskDialog = false
            }
        )
    }

    // Real-time AUTOSAVING Single Task Editor Sheet
    taskToEditByAutosave?.let { task ->
        TaskAutoSaveFormDialog(
            task = task,
            viewModel = viewModel,
            onDismiss = { taskToEditByAutosave = null }
        )
    }

    // Delete task confirmation dialog
    taskToDeleteConfirmByDialog?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDeleteConfirmByDialog = null },
            title = { Text(text = stringResource(id = R.string.delete_confirmation)) },
            text = { Text(text = task.title) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(task)
                        taskToDeleteConfirmByDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(id = R.string.button_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDeleteConfirmByDialog = null }) {
                    Text(text = stringResource(id = R.string.button_cancel))
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val cardColor = if (task.isCompleted) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val elevation = if (task.isCompleted) 0.dp else 2.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("task_item_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                modifier = Modifier
                    .testTag("checkbox_${task.id}")
                    .padding(end = 12.dp)
            )

            // Info column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Priority Badge
                    PriorityIndicator(priority = task.priority)
                }

                if (task.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Category Tag
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (task.isCompleted) 0.3f else 1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Actions group
            IconButton(
                onClick = { onDelete() },
                modifier = Modifier.testTag("delete_button_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.button_delete),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun PriorityIndicator(priority: TaskPriority) {
    val (color, textId) = when (priority) {
        TaskPriority.HIGH -> Color(0xFFE57373) to R.string.priority_high
        TaskPriority.MEDIUM -> Color(0xFFFFB74D) to R.string.priority_medium
        TaskPriority.LOW -> Color(0xFF81C784) to R.string.priority_low
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Text(
            text = stringResource(id = textId),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun TuxEmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val width = size.width
            val height = size.height

            // Tux body shadow back shadow
            drawOval(
                color = Color(0x22000000),
                topLeft = Offset(width * 0.1f, height * 0.85f),
                size = Size(width * 0.8f, height * 0.12f)
            )

            // Outer white wing circles (for flapping shape)
            // Left wing
            drawOval(
                color = Color(0xFF263238),
                topLeft = Offset(width * 0.04f, height * 0.4f),
                size = Size(width * 0.25f, height * 0.4f)
            )
            // Right wing
            drawOval(
                color = Color(0xFF263238),
                topLeft = Offset(width * 0.71f, height * 0.4f),
                size = Size(width * 0.25f, height * 0.4f)
            )

            // Main body curve (Charcoal-black)
            drawOval(
                color = Color(0xFF212121),
                topLeft = Offset(width * 0.15f, height * 0.12f),
                size = Size(width * 0.7f, height * 0.78f)
            )

            // Yellow feet
            drawOval(
                color = Color(0xFFFFB300),
                topLeft = Offset(width * 0.18f, height * 0.8f),
                size = Size(width * 0.28f, height * 0.1f)
            )
            drawOval(
                color = Color(0xFFFFB300),
                topLeft = Offset(width * 0.54f, height * 0.8f),
                size = Size(width * 0.28f, height * 0.1f)
            )

            // White belly
            drawOval(
                color = Color.White,
                topLeft = Offset(width * 0.25f, height * 0.35f),
                size = Size(width * 0.5f, height * 0.5f)
            )

            // White face base (Upper circles)
            drawOval(
                color = Color.White,
                topLeft = Offset(width * 0.31f, height * 0.22f),
                size = Size(width * 0.2f, height * 0.22f)
            )
            drawOval(
                color = Color.White,
                topLeft = Offset(width * 0.49f, height * 0.22f),
                size = Size(width * 0.2f, height * 0.22f)
            )

            // Pupils
            drawOval(
                color = Color.Black,
                topLeft = Offset(width * 0.38f, height * 0.29f),
                size = Size(width * 0.07f, height * 0.08f)
            )
            drawOval(
                color = Color.Black,
                topLeft = Offset(width * 0.55f, height * 0.29f),
                size = Size(width * 0.07f, height * 0.08f)
            )

            // Catchlight
            drawCircle(
                color = Color.White,
                center = Offset(width * 0.395f, height * 0.305f),
                radius = 2f
            )
            drawCircle(
                color = Color.White,
                center = Offset(width * 0.565f, height * 0.305f),
                radius = 2f
            )

            // Beak
            val path = Path().apply {
                moveTo(width * 0.4f, height * 0.4f)
                lineTo(width * 0.6f, height * 0.4f)
                lineTo(width * 0.5f, height * 0.54f)
                close()
            }
            drawPath(
                path = path,
                color = Color(0xFFFF9800)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(id = R.string.empty_lists_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.empty_lists_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun TuxMiniLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Outer penguin circle back
        drawOval(
            color = Color(0xFF212121),
            size = size
        )

        // Belly white
        drawOval(
            color = Color.White,
            topLeft = Offset(width * 0.15f, height * 0.25f),
            size = Size(width * 0.7f, height * 0.7f)
        )

        // Eyes (White)
        drawCircle(
            color = Color.White,
            center = Offset(width * 0.38f, height * 0.28f),
            radius = width * 0.11f
        )
        drawCircle(
            color = Color.White,
            center = Offset(width * 0.62f, height * 0.28f),
            radius = width * 0.11f
        )

        // Pupils (Black)
        drawCircle(
            color = Color.Black,
            center = Offset(width * 0.38f, height * 0.28f),
            radius = width * 0.05f
        )
        drawCircle(
            color = Color.Black,
            center = Offset(width * 0.62f, height * 0.28f),
            radius = width * 0.05f
        )

        // Beak
        val path = Path().apply {
            moveTo(width * 0.42f, height * 0.38f)
            lineTo(width * 0.58f, height * 0.38f)
            lineTo(width * 0.50f, height * 0.52f)
            close()
        }
        drawPath(
            path = path,
            color = Color(0xFFFF9800)
        )
    }
}

@Composable
fun ListFormDialog(
    titleString: String,
    initialList: TodoList? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String) -> Unit
) {
    var name by remember { mutableStateOf(initialList?.name ?: "") }
    var icon by remember { mutableStateOf(initialList?.icon ?: "📝") }
    var touched by remember { mutableStateOf(false) }

    val isNameEmpty = name.trim().isEmpty()
    val icons = listOf("📝", "🐧", "🛒", "💻", "📚", "🎨", "🏠", "🍕", "💡", "⚽", "🎵", "✈️")

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("list_form_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = titleString,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(id = R.string.list_title)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("list_name_input"),
                    singleLine = true,
                    isError = touched && isNameEmpty,
                    supportingText = {
                        if (touched && isNameEmpty) {
                            Text(
                                text = "Название не может быть пустым",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Emoji Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(id = R.string.list_icon),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(icons) { emoji ->
                            val isSelected = icon == emoji
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .clickable { icon = emoji }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDismiss() }
                    ) {
                        Text(text = stringResource(id = R.string.button_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            touched = true
                            if (!isNameEmpty) {
                                onConfirm(name, icon)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = stringResource(id = R.string.button_save))
                    }
                }
            }
        }
    }
}

@Composable
fun TaskFormDialog(
    titleString: String,
    initialTask: Task? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, category: String, priority: TaskPriority) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var category by remember { mutableStateOf(initialTask?.category ?: "Личное") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.MEDIUM) }
    var touched by remember { mutableStateOf(false) }

    val isTitleEmpty = title.trim().isEmpty()

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("task_form_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = titleString,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(text = stringResource(id = R.string.task_title)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    singleLine = true,
                    isError = touched && isTitleEmpty,
                    supportingText = {
                        if (touched && isTitleEmpty) {
                            Text(
                                text = stringResource(id = R.string.task_title_empty_error),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(text = stringResource(id = R.string.task_description)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("task_description_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Category dropdown selection custom row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(id = R.string.task_category),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val categories = listOf("Личное", "Работа", "Учёба", "Linux/Tux")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val active = category == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("chip_select_category_$cat")
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Priority selection row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(id = R.string.task_priority),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val priorities = listOf(
                            TaskPriority.LOW to stringResource(id = R.string.priority_low),
                            TaskPriority.MEDIUM to stringResource(id = R.string.priority_medium),
                            TaskPriority.HIGH to stringResource(id = R.string.priority_high)
                        )
                        priorities.forEach { (prioVal, label) ->
                            val textcolor = when (prioVal) {
                                TaskPriority.LOW -> Color(0xFF66BB6A)
                                TaskPriority.MEDIUM -> Color(0xFFFFA726)
                                TaskPriority.HIGH -> Color(0xFFEF5350)
                            }
                            val active = priority == prioVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (active) textcolor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .clickable { priority = prioVal }
                                    .padding(vertical = 10.dp)
                                    .testTag("chip_select_priority_${prioVal.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(textcolor, CircleShape)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) textcolor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.testTag("cancel_button")
                    ) {
                        Text(text = stringResource(id = R.string.button_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            touched = true
                            if (!isTitleEmpty) {
                                onConfirm(title, description, category, priority)
                            }
                        },
                        modifier = Modifier.testTag("save_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(text = stringResource(id = R.string.button_save))
                    }
                }
            }
        }
    }
}

@Composable
fun TaskAutoSaveFormDialog(
    task: Task,
    viewModel: TaskViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var category by remember { mutableStateOf(task.category) }
    var priority by remember { mutableStateOf(task.priority) }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("task_autosave_dialog"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.edit_task),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Autosaved",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Автосохранение",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Title field (auto-saved)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.trim().isNotEmpty()) {
                            viewModel.updateTask(task.copy(title = it.trim()))
                        }
                    },
                    label = { Text(text = stringResource(id = R.string.task_title)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Description field (auto-saved)
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        viewModel.updateTask(task.copy(description = it.trim()))
                    },
                    label = { Text(text = stringResource(id = R.string.task_description)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("task_description_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Category selection (auto-saved)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(id = R.string.task_category),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val categories = listOf("Личное", "Работа", "Учёба", "Linux/Tux")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { cat ->
                            val active = category == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable {
                                        category = cat
                                        viewModel.updateTask(task.copy(category = cat))
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("chip_select_category_$cat")
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Priority selection (auto-saved)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(id = R.string.task_priority),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val priorities = listOf(
                            TaskPriority.LOW to stringResource(id = R.string.priority_low),
                            TaskPriority.MEDIUM to stringResource(id = R.string.priority_medium),
                            TaskPriority.HIGH to stringResource(id = R.string.priority_high)
                        )
                        priorities.forEach { (prioVal, label) ->
                            val textcolor = when (prioVal) {
                                TaskPriority.LOW -> Color(0xFF66BB6A)
                                TaskPriority.MEDIUM -> Color(0xFFFFA726)
                                TaskPriority.HIGH -> Color(0xFFEF5350)
                            }
                            val active = priority == prioVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (active) textcolor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .clickable {
                                        priority = prioVal
                                        viewModel.updateTask(task.copy(priority = prioVal))
                                    }
                                    .padding(vertical = 10.dp)
                                    .testTag("chip_select_priority_${prioVal.name}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(textcolor, CircleShape)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) textcolor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Done Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { onDismiss() },
                        modifier = Modifier.testTag("close_autosave_button")
                    ) {
                        Text(text = "Готово")
                    }
                }
            }
        }
    }
}
