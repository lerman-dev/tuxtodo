package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Task
import com.example.data.TaskPriority
import com.example.data.TaskRepository
import com.example.data.TodoList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Все")
    val selectedStatus = MutableStateFlow("Все")

    val currentListId = MutableStateFlow<Long?>(null)

    val allLists: StateFlow<List<TodoList>> = repository.allLists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTasks: StateFlow<List<Task>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredTasks: StateFlow<List<Task>> = combine(
        currentListId.flatMapLatest { listId ->
            if (listId == null) {
                flowOf(emptyList())
            } else {
                repository.getTasksForList(listId)
            }
        },
        searchQuery,
        selectedCategory,
        selectedStatus
    ) { tasks, query, category, status ->
        tasks.filter { task ->
            // Search query matches title or description
            val matchesQuery = task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)

            // Category matches
            val matchesCategory = category == "Все" || task.category == category

            // Status matches
            val matchesStatus = when (status) {
                "Активные" -> !task.isCompleted
                "Завершённые" -> task.isCompleted
                else -> true
            }

            matchesQuery && matchesCategory && matchesStatus
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Seed default items if db is completely empty
        viewModelScope.launch {
            repository.allLists.firstOrNull()?.let { lists ->
                if (lists.isEmpty()) {
                    // Create default list points
                    val listId1 = repository.insertList(TodoList(name = "🐧 Задачи от Tux", icon = "🐧"))
                    val listId2 = repository.insertList(TodoList(name = "🛒 Покупки", icon = "🛒"))

                    // Seed list 1 tasks
                    repository.insert(Task(
                        listId = listId1,
                        title = "Установить Arch Linux",
                        description = "Да-да, btw I use Arch! Настроить красивые шрифты и терминал.",
                        category = "Linux/Tux",
                        priority = TaskPriority.HIGH
                    ))
                    repository.insert(Task(
                        listId = listId1,
                        title = "Покормить пингвина",
                        description = "Рыбой, свежей и сочной. Пингвин Tux ждёт!",
                        category = "Личное",
                        priority = TaskPriority.HIGH
                    ))
                    repository.insert(Task(
                        listId = listId1,
                        title = "Обновить систему",
                        description = "Запустить sudo pacman -Syu всей душой.",
                        category = "Linux/Tux",
                        priority = TaskPriority.MEDIUM
                    ))

                    // Seed list 2 tasks
                    repository.insert(Task(
                        listId = listId2,
                        title = "Свежая рыба для Tux",
                        description = "Купить лосось или тунец.",
                        category = "Личное",
                        priority = TaskPriority.MEDIUM
                    ))
                    repository.insert(Task(
                        listId = listId2,
                        title = "Кофе в зернах",
                        description = "Светлая или средняя обжарка арабики.",
                        category = "Личное",
                        priority = TaskPriority.LOW
                    ))
                }
            }
        }
    }

    fun addList(name: String, icon: String) {
        viewModelScope.launch {
            repository.insertList(TodoList(name = name.trim(), icon = icon))
        }
    }

    fun updateList(todoList: TodoList) {
        viewModelScope.launch {
            repository.updateList(todoList)
        }
    }

    fun deleteList(todoList: TodoList) {
        viewModelScope.launch {
            repository.deleteList(todoList)
        }
    }

    fun addTask(title: String, description: String, category: String, priority: TaskPriority) {
        val listId = currentListId.value ?: return
        viewModelScope.launch {
            repository.insert(
                Task(
                    listId = listId,
                    title = title.trim(),
                    description = description.trim(),
                    category = category,
                    priority = priority
                )
            )
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.update(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.update(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }
}

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
