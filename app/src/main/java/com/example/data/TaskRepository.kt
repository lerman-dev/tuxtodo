package com.example.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allLists: Flow<List<TodoList>> = taskDao.getAllLists()

    fun getTasksForList(listId: Long): Flow<List<Task>> = taskDao.getTasksForList(listId)

    suspend fun insert(task: Task): Long {
        return taskDao.insertTask(task)
    }

    suspend fun update(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun delete(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteById(id: Long) {
        taskDao.deleteTaskById(id)
    }

    suspend fun insertList(todoList: TodoList): Long {
        return taskDao.insertList(todoList)
    }

    suspend fun updateList(todoList: TodoList) {
        taskDao.updateList(todoList)
    }

    suspend fun deleteList(todoList: TodoList) {
        taskDao.deleteTasksByListId(todoList.id)
        taskDao.deleteList(todoList)
    }
}
