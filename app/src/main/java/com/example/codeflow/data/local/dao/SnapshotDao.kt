package com.example.codeflow.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.codeflow.data.local.entity.FileSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for [FileSnapshot] operations.
 */
@Dao
interface SnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: FileSnapshot): Long

    @Query("SELECT * FROM file_snapshots WHERE fileId = :fileId ORDER BY versionNumber ASC")
    fun getSnapshotsForFile(fileId: Long): Flow<List<FileSnapshot>>

    @Query("SELECT * FROM file_snapshots WHERE fileId = :fileId ORDER BY versionNumber ASC")
    suspend fun getSnapshotsForFileSync(fileId: Long): List<FileSnapshot>

    @Query("SELECT * FROM file_snapshots WHERE fileId = :fileId ORDER BY versionNumber DESC LIMIT 1")
    suspend fun getLatestSnapshotForFile(fileId: Long): FileSnapshot?

    @Delete
    suspend fun deleteSnapshot(snapshot: FileSnapshot): Int
}
