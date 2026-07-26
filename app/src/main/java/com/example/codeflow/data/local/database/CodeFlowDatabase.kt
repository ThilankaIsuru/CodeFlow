package com.example.codeflow.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.codeflow.data.local.dao.FileDao
import com.example.codeflow.data.local.dao.SnapshotDao
import com.example.codeflow.data.local.entity.FileSnapshot
import com.example.codeflow.data.local.entity.ProjectFile

/**
 * Main Room Database configuration for CodeFlow.
 * Handles schema mapping for [ProjectFile] and [FileSnapshot] entities.
 */
@Database(
    entities = [
        ProjectFile::class,
        FileSnapshot::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CodeFlowDatabase : RoomDatabase() {

    abstract fun fileDao(): FileDao
    abstract fun snapshotDao(): SnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: CodeFlowDatabase? = null

        const val DATABASE_NAME = "codeflow_database.db"

        fun getInstance(context: Context): CodeFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CodeFlowDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
