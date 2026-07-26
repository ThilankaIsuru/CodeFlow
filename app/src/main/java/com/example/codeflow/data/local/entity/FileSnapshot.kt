package com.example.codeflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity storing incremental delta text patches for tracked files.
 * Linked to [ProjectFile] via foreign key with CASCADE deletion.
 */
@Entity(
    tableName = "file_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = ProjectFile::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["fileId"])
    ]
)
data class FileSnapshot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileId: Long,
    val versionNumber: Int,
    val versionName: String,
    val deltaPatch: String,
    val timestamp: Long = System.currentTimeMillis()
)
