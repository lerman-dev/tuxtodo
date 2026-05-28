package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // Tasks queries
    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY isCompleted ASC, priority DESC, createdAt DESC")
    fun getTasksForList(listId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority DESC, createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks WHERE listId = :listId")
    suspend fun deleteTasksByListId(listId: Long)

    // TodoLists queries
    @Query("SELECT * FROM todo_lists ORDER BY createdAt DESC")
    fun getAllLists(): Flow<List<TodoList>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(todoList: TodoList): Long

    @Update
    suspend fun updateList(todoList: TodoList)

    @Delete
    suspend fun deleteList(todoList: TodoList)
}
