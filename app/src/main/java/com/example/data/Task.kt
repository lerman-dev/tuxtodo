package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

enum class TaskPriority {
    LOW, MEDIUM, HIGH
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Личное", // Личное, Работа, Учёба, Linux/Tux
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
