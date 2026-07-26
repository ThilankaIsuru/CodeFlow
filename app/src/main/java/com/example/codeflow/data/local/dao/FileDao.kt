package com.example.codeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.codeflow.data.local.entity.ProjectFile
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [ProjectFile] operations.
 */
@Dao
interface FileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFile): Long

    @Update
    suspend fun updateFile(file: ProjectFile): Int

    @Query("UPDATE project_files SET isReadOnly = :isReadOnly WHERE id = :id")
    suspend fun updateIsReadOnly(id: Long, isReadOnly: Boolean): Int

    @Delete
    suspend fun deleteFile(file: ProjectFile): Int

    @Query("SELECT * FROM project_files WHERE absolutePath = :absolutePath LIMIT 1")
    suspend fun getFileByAbsolutePath(absolutePath: String): ProjectFile?

    @Query("SELECT * FROM project_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Long): ProjectFile?

    @Query("SELECT * FROM project_files ORDER BY createdAt DESC")
    fun getAllFiles(): Flow<List<ProjectFile>>
}
