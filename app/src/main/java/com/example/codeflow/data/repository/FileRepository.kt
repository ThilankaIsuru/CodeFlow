package com.example.codeflow.data.repository

import android.content.Context
import com.example.codeflow.data.local.dao.FileDao
import com.example.codeflow.data.local.dao.SnapshotDao
import com.example.codeflow.data.local.entity.FileSnapshot
import com.example.codeflow.data.local.entity.ProjectFile
import com.example.codeflow.ui.editor.RecentFileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository performing asynchronous, secure local File I/O operations,
 * incremental snapshot versioning, and synchronizing metadata with Room persistence DB.
 */
class FileRepository(
    private val context: Context,
    private val fileDao: FileDao,
    private val snapshotDao: SnapshotDao
) {
    private val backupFile = File(context.filesDir, ".crash_recovery_backup.tmp")
    private val backupMetaFile = File(context.filesDir, ".crash_recovery_meta.tmp")

    fun getFilesDir(): File = context.filesDir

    /**
     * Reads text content from local file or content:// URI using specified character encoding off the main thread.
     */
    suspend fun readFile(
        path: String,
        encodingName: String = "UTF-8"
    ): Result<String> = withContext(Dispatchers.IO) {
        readUriOrFile(path, encodingName).map { it.first }
    }

    private fun getMirrorFile(fileName: String): File {
        val dir = File(context.filesDir, "user_documents")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, fileName)
    }

    /**
     * Reads text content and display name from file path or content:// URI with local mirror fallback.
     */
    suspend fun readUriOrFile(
        pathOrUri: String,
        encodingName: String = "UTF-8"
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val charset = getCharset(encodingName)
            if (pathOrUri.startsWith("content://")) {
                val uri = android.net.Uri.parse(pathOrUri)
                val displayName = getUriDisplayName(uri) ?: getFileNameFromUriPath(pathOrUri) ?: "OpenedDocument.txt"
                val mirror = getMirrorFile(displayName)

                try {
                    val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (_: Exception) {}

                val content = try {
                    val readText = context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes().toString(charset)
                    } ?: throw java.io.FileNotFoundException("Could not open input stream for URI: $pathOrUri")

                    try { mirror.writeText(readText, charset) } catch (_: Exception) {}
                    readText
                } catch (se: Throwable) {
                    if (mirror.exists()) {
                        mirror.readText(charset)
                    } else {
                        throw IllegalStateException("URI Access Expired. Please re-open file using system file picker.")
                    }
                }
                Pair(content, displayName)
            } else {
                val file = File(pathOrUri)
                if (!file.exists()) {
                    throw java.io.FileNotFoundException("File does not exist: $pathOrUri")
                }
                Pair(file.readText(charset), file.name)
            }
        }
    }

    /**
     * Writes text content to local file path or content:// URI off the main thread.
     * Enforces read-only lock check tied to Room DB before performing write.
     */
    suspend fun writeFile(
        path: String,
        content: String,
        encodingName: String = "UTF-8"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        writeUriOrFile(path, content, encodingName)
    }

    /**
     * Writes text content to local file or content:// URI with local mirror sync.
     */
    suspend fun writeUriOrFile(
        pathOrUri: String,
        content: String,
        encodingName: String = "UTF-8"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dbFile = fileDao.getFileByAbsolutePath(pathOrUri)
            if (dbFile != null && dbFile.isReadOnly) {
                throw IllegalStateException("File is marked as Read-Only in database. Modification blocked.")
            }

            val charset = getCharset(encodingName)
            if (pathOrUri.startsWith("content://")) {
                val uri = android.net.Uri.parse(pathOrUri)
                val displayName = getUriDisplayName(uri) ?: "Document.txt"
                val mirror = getMirrorFile(displayName)

                try {
                    val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (_: Exception) {}

                try {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                        stream.write(content.toByteArray(charset))
                    } ?: throw java.io.FileNotFoundException("Could not open output stream for URI: $pathOrUri")
                } catch (se: SecurityException) {
                    mirror.writeText(content, charset)
                }

                try { mirror.writeText(content, charset) } catch (_: Exception) {}

                trackFile(path = pathOrUri, fileName = displayName, encodingName = encodingName)
            } else {
                val file = File(pathOrUri)
                val parent = file.parentFile
                if (parent != null && !parent.exists()) {
                    parent.mkdirs()
                }
                file.writeText(content, charset)
                trackFile(path = pathOrUri, fileName = file.name, encodingName = encodingName)
            }
            Unit
        }
    }

    private fun getUriDisplayName(uri: android.net.Uri): String? {
        return try {
            var name: String? = null
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
            name
        } catch (_: Exception) {
            null
        }
    }

    private fun getFileNameFromUriPath(uriString: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriString)
            val lastSegment = uri.lastPathSegment
            if (lastSegment != null && lastSegment.contains("/")) {
                lastSegment.substringAfterLast("/")
            } else {
                lastSegment
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Tracks file in Room DB to keep recent files list populated accurately.
     */
    suspend fun trackFile(
        path: String,
        fileName: String,
        encodingName: String = "UTF-8"
    ): Long = withContext(Dispatchers.IO) {
        val existing = fileDao.getFileByAbsolutePath(path)
        if (existing != null) {
            val updated = existing.copy(
                fileName = fileName,
                encoding = encodingName,
                createdAt = System.currentTimeMillis()
            )
            fileDao.updateFile(updated)
            existing.id
        } else {
            val newFile = ProjectFile(
                fileName = fileName,
                absolutePath = path,
                encoding = encodingName,
                isReadOnly = false,
                createdAt = System.currentTimeMillis()
            )
            fileDao.insertFile(newFile)
        }
    }

    /**
     * Flow of recent files formatted for UI list display.
     */
    fun getRecentFiles(): Flow<List<RecentFileItem>> {
        return fileDao.getAllFiles().map { files ->
            files.map { file ->
                RecentFileItem(
                    id = file.id,
                    fileName = file.fileName,
                    path = file.absolutePath,
                    relativeTimestamp = formatTimestamp(file.createdAt)
                )
            }
        }
    }

    /**
     * Updates read-only state flag in Room DB.
     */
    suspend fun updateReadOnlyState(fileId: Long, isReadOnly: Boolean) = withContext(Dispatchers.IO) {
        fileDao.updateIsReadOnly(fileId, isReadOnly)
    }

    // --- INCREMENTAL DELTA SNAPSHOT VERSIONING ---

    /**
     * Creates a new incremental delta snapshot for the specified [fileId].
     */
    suspend fun createSnapshot(
        fileId: Long,
        versionName: String,
        currentContent: String
    ): Result<FileSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val projectFile = fileDao.getFileById(fileId)
                ?: throw IllegalArgumentException("ProjectFile with id $fileId not found")

            val existingSnapshots = snapshotDao.getSnapshotsForFileSync(fileId)

            val (lastText, nextVersion) = if (existingSnapshots.isEmpty()) {
                Pair("", 1)
            } else {
                val latest = existingSnapshots.last()
                val reconstructed = DeltaSnapshotEngine.reconstructVersion(
                    snapshots = existingSnapshots,
                    targetVersion = latest.versionNumber
                ).getOrDefault("")
                Pair(reconstructed, latest.versionNumber + 1)
            }

            val patchString = DeltaSnapshotEngine.computeUnifiedDiff(
                fileName = projectFile.fileName,
                oldContent = lastText,
                newContent = currentContent
            )

            val snapshot = FileSnapshot(
                fileId = fileId,
                versionNumber = nextVersion,
                versionName = versionName,
                deltaPatch = patchString,
                timestamp = System.currentTimeMillis()
            )

            val newId = snapshotDao.insertSnapshot(snapshot)
            snapshot.copy(id = newId)
        }
    }

    /**
     * Flow of version snapshots for a specific file.
     */
    fun getSnapshotsForFile(fileId: Long): Flow<List<FileSnapshot>> {
        return snapshotDao.getSnapshotsForFile(fileId)
    }

    /**
     * Reconstructs full document text for a target version number.
     */
    suspend fun reconstructVersion(
        fileId: Long,
        targetVersion: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshots = snapshotDao.getSnapshotsForFileSync(fileId)
            DeltaSnapshotEngine.reconstructVersion(snapshots, targetVersion).getOrThrow()
        }
    }

    // --- CRASH RECOVERY CACHE ---

    suspend fun saveCrashBackup(
        content: String,
        path: String?,
        fileName: String
    ) = withContext(Dispatchers.IO) {
        runCatching {
            backupFile.writeText(content, Charsets.UTF_8)
            val metaString = "${path ?: ""}|$fileName"
            backupMetaFile.writeText(metaString, Charsets.UTF_8)
        }
    }

    suspend fun hasCrashBackup(): Boolean = withContext(Dispatchers.IO) {
        backupFile.exists() && backupFile.length() > 0
    }

    suspend fun loadCrashBackup(): Pair<String, String>? = withContext(Dispatchers.IO) {
        if (!hasCrashBackup()) return@withContext null
        runCatching {
            val content = backupFile.readText(Charsets.UTF_8)
            val meta = if (backupMetaFile.exists()) backupMetaFile.readText(Charsets.UTF_8) else ""
            val parts = meta.split("|")
            val fileName = parts.getOrNull(1)?.ifBlank { null } ?: "Restored_Session.kt"
            Pair(content, fileName)
        }.getOrNull()
    }

    suspend fun clearCrashBackup() = withContext(Dispatchers.IO) {
        runCatching {
            if (backupFile.exists()) backupFile.delete()
            if (backupMetaFile.exists()) backupMetaFile.delete()
        }
    }

    private fun getCharset(name: String): Charset {
        return try {
            Charset.forName(name)
        } catch (_: Exception) {
            Charsets.UTF_8
        }
    }

    private fun formatTimestamp(timeMs: Long): String {
        val diffMs = System.currentTimeMillis() - timeMs
        val diffHours = diffMs / (1000 * 60 * 60)
        return when {
            diffHours < 1 -> "Just now"
            diffHours < 24 -> "$diffHours hours ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timeMs))
        }
    }
}
