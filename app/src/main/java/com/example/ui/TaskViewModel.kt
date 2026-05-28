package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Task
import com.example.data.TaskPriority
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("Все")
    val selectedStatus = MutableStateFlow("Все")

    val filteredTasks: StateFlow<List<Task>> = combine(
        repository.allTasks,
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
            repository.allTasks.firstOrNull()?.let { tasks ->
                if (tasks.isEmpty()) {
                    repository.insert(Task(
                        title = "Установить Arch Linux",
                        description = "Да-да, btw I use Arch! Настроить красивые шрифты и терминал.",
                        category = "Linux/Tux",
                        priority = TaskPriority.HIGH
                    ))
                    repository.insert(Task(
                        title = "Покормить пингвина",
                        description = "Рыбой, свежей и сочной. Пингвин Tux ждёт!",
                        category = "Личное",
                        priority = TaskPriority.HIGH
                    ))
                    repository.insert(Task(
                        title = "Обновить систему",
                        description = "Запустить sudo pacman -Syu всей душой.",
                        category = "Linux/Tux",
                        priority = TaskPriority.MEDIUM
                    ))
                    repository.insert(Task(
                        title = "Сдать лабораторную №3",
                        description = "Написать отчет по операционным системам про управление памятью.",
                        category = "Учёба",
                        priority = TaskPriority.MEDIUM
                    ))
                    repository.insert(Task(
                        title = "Проверить пулреквесты",
                        description = "Отревьюить новые коммиты в репозитории TuxTodo.",
                        category = "Работа",
                        priority = TaskPriority.LOW
                    ))
                }
            }
        }
    }

    fun addTask(title: String, description: String, category: String, priority: TaskPriority) {
        viewModelScope.launch {
            repository.insert(
                Task(
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
