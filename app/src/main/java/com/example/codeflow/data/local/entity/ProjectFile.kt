package com.example.codeflow.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a primary file tracked by CodeFlow.
 * Uses a unique index on absolutePath to prevent duplicate file entries.
 */
@Entity(
    tableName = "project_files",
    indices = [
        Index(value = ["absolutePath"], unique = true)
    ]
)
data class ProjectFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val absolutePath: String,
    val encoding: String = "UTF-8",
    val isReadOnly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
