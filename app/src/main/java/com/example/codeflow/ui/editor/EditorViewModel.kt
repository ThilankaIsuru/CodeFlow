package com.example.codeflow.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.codeflow.data.local.entity.FileSnapshot
import com.example.codeflow.data.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel for CodeFlow text editor.
 * Manages UI StateFlow, Undo/Redo session manager, secure File I/O,
 * background crash prevention, and incremental delta versioning engine.
 */
class EditorViewModel(
    private val repository: FileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val undoRedoManager = UndoRedoManager()
    private var snapshotsObserverJob: Job? = null

    init {
        // 1. Observe Recent Files from Room DB
        viewModelScope.launch {
            repository.getRecentFiles().collect { recentItems ->
                _uiState.update { it.copy(recentFiles = recentItems) }
            }
        }

        // 2. Initial Crash Recovery Check on Startup
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.hasCrashBackup()) {
                _uiState.update { it.copy(hasRecoveryBackup = true) }
            }
        }

        // 3. Automated 10-Second Crash Prevention Loop
        startCrashSaverLoop()
    }

    private fun startCrashSaverLoop() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(10_000)
                val currentState = _uiState.value
                if (currentState.isModified) {
                    repository.saveCrashBackup(
                        content = currentState.content.text,
                        path = currentState.absolutePath,
                        fileName = currentState.fileName
                    )
                }
            }
        }
    }

    private fun observeSnapshots(fileId: Long) {
        snapshotsObserverJob?.cancel()
        snapshotsObserverJob = viewModelScope.launch {
            repository.getSnapshotsForFile(fileId).collect { snapshotList ->
                _uiState.update { it.copy(snapshots = snapshotList) }
            }
        }
    }

    fun onContentChange(newContent: TextFieldValue) {
        val previousText = _uiState.value.content.text
        if (newContent.text != previousText) {
            undoRedoManager.pushState(previousText)
        }
        _uiState.update {
            it.copy(
                content = newContent,
                isModified = true,
                canUndo = undoRedoManager.canUndo,
                canRedo = undoRedoManager.canRedo
            )
        }
    }

    fun insertSymbol(symbol: String) {
        val currentText = _uiState.value.content.text
        val selection = _uiState.value.content.selection
        val start = selection.min
        val end = selection.max

        undoRedoManager.pushState(currentText)

        val newText = StringBuilder(currentText)
            .replace(start, end, symbol)
            .toString()
        val newCursorPos = start + symbol.length

        _uiState.update {
            it.copy(
                content = TextFieldValue(
                    text = newText,
                    selection = TextRange(newCursorPos)
                ),
                isModified = true,
                canUndo = undoRedoManager.canUndo,
                canRedo = undoRedoManager.canRedo
            )
        }
    }

    fun undo() {
        val currentText = _uiState.value.content.text
        val previousState = undoRedoManager.undo(currentText) ?: return
        _uiState.update {
            it.copy(
                content = TextFieldValue(
                    text = previousState,
                    selection = TextRange(previousState.length)
                ),
                isModified = true,
                canUndo = undoRedoManager.canUndo,
                canRedo = undoRedoManager.canRedo
            )
        }
    }

    fun redo() {
        val currentText = _uiState.value.content.text
        val nextState = undoRedoManager.redo(currentText) ?: return
        _uiState.update {
            it.copy(
                content = TextFieldValue(
                    text = nextState,
                    selection = TextRange(nextState.length)
                ),
                isModified = true,
                canUndo = undoRedoManager.canUndo,
                canRedo = undoRedoManager.canRedo
            )
        }
    }

    fun newFile() {
        undoRedoManager.clear()
        snapshotsObserverJob?.cancel()
        _uiState.update {
            it.copy(
                fileName = "Untitled.kt",
                absolutePath = null,
                activeFileId = null,
                content = TextFieldValue(""),
                isModified = false,
                canUndo = false,
                canRedo = false,
                snapshots = emptyList(),
                statusMessage = "Created new file"
            )
        }
    }

    fun clearTriggerOpenPicker() {
        _uiState.update { it.copy(shouldTriggerOpenPicker = false) }
    }

    fun openFile(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val encoding = _uiState.value.encoding
            val result = repository.readUriOrFile(path, encoding)
            result.onSuccess { (contentString, displayName) ->
                val trackedId = repository.trackFile(path, displayName, encoding)
                undoRedoManager.clear()

                _uiState.update {
                    it.copy(
                        fileName = displayName,
                        absolutePath = path,
                        activeFileId = trackedId,
                        content = TextFieldValue(contentString),
                        isModified = false,
                        canUndo = false,
                        canRedo = false,
                        statusMessage = "Opened $displayName"
                    )
                }
                observeSnapshots(trackedId)
            }.onFailure { error ->
                handleOpenFileFailure(error)
            }
        }
    }

    fun saveFile() {
        val path = _uiState.value.absolutePath ?: java.io.File(repository.getFilesDir(), _uiState.value.fileName).absolutePath
        performSave(path, _uiState.value.fileName)
    }

    fun openFileFromUri(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val encoding = _uiState.value.encoding
            val result = repository.readUriOrFile(uriString, encoding)
            result.onSuccess { (contentString, displayName) ->
                val trackedId = repository.trackFile(uriString, displayName, encoding)
                undoRedoManager.clear()

                _uiState.update {
                    it.copy(
                        fileName = displayName,
                        absolutePath = uriString,
                        activeFileId = trackedId,
                        content = TextFieldValue(contentString),
                        isModified = false,
                        canUndo = false,
                        canRedo = false,
                        statusMessage = "Opened $displayName"
                    )
                }
                observeSnapshots(trackedId)
            }.onFailure { error ->
                handleOpenFileFailure(error)
            }
        }
    }

    private fun handleOpenFileFailure(error: Throwable) {
        val msg = error.message ?: ""
        if (msg.contains("Permission Denial", ignoreCase = true) ||
            msg.contains("URI Access Expired", ignoreCase = true) ||
            msg.contains("requires that you obtain access", ignoreCase = true)) {
            _uiState.update {
                it.copy(
                    shouldTriggerOpenPicker = true,
                    statusMessage = "URI permission expired. Opening system file picker..."
                )
            }
        } else {
            _uiState.update { it.copy(statusMessage = "Failed to open file: ${error.message}") }
        }
    }

    fun saveFileToUri(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentString = _uiState.value.content.text
            val encoding = _uiState.value.encoding
            val result = repository.writeUriOrFile(uriString, contentString, encoding)

            result.onSuccess {
                repository.clearCrashBackup()
                val displayName = repository.readUriOrFile(uriString, encoding).getOrNull()?.second ?: _uiState.value.fileName
                val trackedId = repository.trackFile(uriString, displayName, encoding)

                _uiState.update {
                    it.copy(
                        fileName = displayName,
                        absolutePath = uriString,
                        activeFileId = trackedId,
                        isModified = false,
                        statusMessage = "Successfully saved $displayName"
                    )
                }
                observeSnapshots(trackedId)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(statusMessage = "Save Failed: ${error.message}")
                }
            }
        }
    }

    fun saveFileAs(newPath: String) {
        val file = java.io.File(newPath)
        performSave(newPath, file.name)
    }

    private fun performSave(path: String, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentString = _uiState.value.content.text
            val encoding = _uiState.value.encoding
            val result = repository.writeFile(path, contentString, encoding)

            result.onSuccess {
                repository.clearCrashBackup()
                val trackedId = repository.trackFile(path, fileName, encoding)

                _uiState.update {
                    it.copy(
                        fileName = fileName,
                        absolutePath = path,
                        activeFileId = trackedId,
                        isModified = false,
                        statusMessage = "Successfully saved $fileName"
                    )
                }
                observeSnapshots(trackedId)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(statusMessage = "Save Failed: ${error.message}")
                }
            }
        }
    }

    // --- PHASE 5 VERSIONING & DIFF VIEWER ACTIONS ---

    fun showVersionHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val validPath = _uiState.value.absolutePath ?: java.io.File(repository.getFilesDir(), _uiState.value.fileName).absolutePath
            val targetFile = java.io.File(validPath)
            if (!targetFile.exists()) {
                repository.writeFile(validPath, _uiState.value.content.text, _uiState.value.encoding)
            }
            val fileId = repository.trackFile(validPath, _uiState.value.fileName, _uiState.value.encoding)
            _uiState.update { it.copy(activeFileId = fileId, absolutePath = validPath) }
            observeSnapshots(fileId)
            _uiState.update { it.copy(isVersionHistoryOpen = true) }
        }
    }

    fun closeVersionHistory() {
        _uiState.update { it.copy(isVersionHistoryOpen = false) }
    }

    fun createVersionSnapshot(versionName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val validPath = _uiState.value.absolutePath ?: java.io.File(repository.getFilesDir(), _uiState.value.fileName).absolutePath
            val targetFile = java.io.File(validPath)
            if (!targetFile.exists()) {
                repository.writeFile(validPath, _uiState.value.content.text, _uiState.value.encoding)
            }
            val fileId = repository.trackFile(validPath, _uiState.value.fileName, _uiState.value.encoding)
            _uiState.update { it.copy(activeFileId = fileId, absolutePath = validPath) }

            val currentContent = _uiState.value.content.text
            val result = repository.createSnapshot(fileId, versionName, currentContent)

            result.onSuccess { snapshot ->
                _uiState.update {
                    it.copy(statusMessage = "Created snapshot v${snapshot.versionNumber} (${snapshot.versionName})")
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(statusMessage = "Snapshot Creation Failed: ${error.message}")
                }
            }
        }
    }

    fun formatCode() {
        val currentText = _uiState.value.content.text
        val formatted = formatKotlinCode(currentText)
        if (formatted != currentText) {
            undoRedoManager.pushState(currentText)
            _uiState.update {
                it.copy(
                    content = TextFieldValue(formatted),
                    isModified = true,
                    statusMessage = "Formatted code"
                )
            }
        } else {
            _uiState.update { it.copy(statusMessage = "Code already formatted") }
        }
    }

    fun toggleMarkdownPreview() {
        _uiState.update {
            val nextState = !it.isPreviewMode
            it.copy(
                isPreviewMode = nextState,
                statusMessage = if (nextState) "Markdown Preview enabled" else "Markdown Preview disabled"
            )
        }
    }

    private fun formatKotlinCode(code: String): String {
        val lines = code.lines()
        var indentLevel = 0
        val result = mutableListOf<String>()

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                result.add("")
                continue
            }

            if (trimmed.startsWith("}") || trimmed.startsWith(")") || trimmed.startsWith("]")) {
                indentLevel = maxOf(0, indentLevel - 1)
            }

            val indent = "    ".repeat(indentLevel)
            result.add(indent + trimmed)

            val openBraces = trimmed.count { it == '{' || it == '(' || it == '[' }
            val closeBraces = trimmed.count { it == '}' || it == ')' || it == ']' }
            val netChange = openBraces - closeBraces

            if (!trimmed.startsWith("}") && !trimmed.startsWith(")") && !trimmed.startsWith("]")) {
                indentLevel = maxOf(0, indentLevel + netChange)
            }
        }
        return result.joinToString("\n")
    }

    fun compareDiff(snapshot: FileSnapshot) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileId = snapshot.fileId
            val result = repository.reconstructVersion(fileId, snapshot.versionNumber)
            val oldText = result.getOrElse {
                snapshot.deltaPatch.lines()
                    .filterNot { l -> l.startsWith("---") || l.startsWith("+++") || l.startsWith("@@") }
                    .map { l -> if (l.startsWith("+")) l.substring(1) else l }
                    .joinToString("\n")
            }

            _uiState.update {
                it.copy(
                    diffOldText = oldText,
                    diffNewText = _uiState.value.content.text,
                    selectedVersionName = "v${snapshot.versionNumber} - ${snapshot.versionName}",
                    isVersionHistoryOpen = false,
                    isDiffViewerOpen = true
                )
            }
        }
    }

    fun closeDiffViewer() {
        _uiState.update {
            it.copy(
                isDiffViewerOpen = false,
                isVersionHistoryOpen = true,
                diffOldText = null,
                diffNewText = null,
                selectedVersionName = null
            )
        }
    }

    fun restoreVersion(snapshot: FileSnapshot) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileId = snapshot.fileId
            val result = repository.reconstructVersion(fileId, snapshot.versionNumber)

            result.onSuccess { reconstructedText ->
                _uiState.update {
                    it.copy(
                        content = TextFieldValue(reconstructedText),
                        isModified = true,
                        isVersionHistoryOpen = false,
                        statusMessage = "Restored version v${snapshot.versionNumber} (${snapshot.versionName})"
                    )
                }
                val path = _uiState.value.absolutePath
                if (path != null) {
                    repository.writeFile(path, reconstructedText, _uiState.value.encoding)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(statusMessage = "Restore Failed: ${error.message}")
                }
            }
        }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(isSettingsOpen = !it.isSettingsOpen) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun setFontSize(sizeSp: Int) {
        _uiState.update { it.copy(fontSizeSp = sizeSp) }
    }

    fun toggleLineNumbers() {
        _uiState.update { it.copy(showLineNumbers = !it.showLineNumbers) }
    }

    fun toggleWordWrap() {
        _uiState.update { it.copy(wordWrap = !it.wordWrap) }
    }

    fun toggleSearch() {
        _uiState.update { it.copy(isSearchActive = !it.isSearchActive) }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onReplaceQueryChange(query: String) {
        _uiState.update { it.copy(replaceQuery = query) }
    }

    fun replaceNext() {
        val find = _uiState.value.searchQuery
        val replace = _uiState.value.replaceQuery
        if (find.isBlank()) return

        val currentText = _uiState.value.content.text
        val index = currentText.indexOf(find, ignoreCase = true)
        if (index != -1) {
            undoRedoManager.pushState(currentText)
            val newText = StringBuilder(currentText)
                .replace(index, index + find.length, replace)
                .toString()
            val nextCursor = index + replace.length

            _uiState.update {
                it.copy(
                    content = TextFieldValue(
                        text = newText,
                        selection = TextRange(nextCursor)
                    ),
                    isModified = true,
                    canUndo = undoRedoManager.canUndo,
                    canRedo = undoRedoManager.canRedo,
                    statusMessage = "Replaced 1 match"
                )
            }
        } else {
            _uiState.update { it.copy(statusMessage = "No match found") }
        }
    }

    fun replaceAll() {
        val find = _uiState.value.searchQuery
        val replace = _uiState.value.replaceQuery
        if (find.isBlank()) return

        val currentText = _uiState.value.content.text
        val regex = Regex(Regex.escape(find), RegexOption.IGNORE_CASE)
        val matchCount = regex.findAll(currentText).count()

        if (matchCount > 0) {
            undoRedoManager.pushState(currentText)
            val newText = regex.replace(currentText, replace)
            _uiState.update {
                it.copy(
                    content = TextFieldValue(
                        text = newText,
                        selection = TextRange(newText.length)
                    ),
                    isModified = true,
                    canUndo = undoRedoManager.canUndo,
                    canRedo = undoRedoManager.canRedo,
                    statusMessage = "Replaced $matchCount occurrences"
                )
            }
        } else {
            _uiState.update { it.copy(statusMessage = "No matches found") }
        }
    }

    fun closeSearch() {
        _uiState.update { it.copy(isSearchActive = false, searchQuery = "", replaceQuery = "") }
    }

    fun toggleReadOnly() {
        val newReadOnly = !_uiState.value.isReadOnly
        val activeId = _uiState.value.activeFileId

        _uiState.update { it.copy(isReadOnly = newReadOnly) }

        if (activeId != null) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateReadOnlyState(activeId, newReadOnly)
            }
        }
    }

    fun toggleEncoding() {
        val newEncoding = if (_uiState.value.encoding == "UTF-8") "ASCII" else "UTF-8"
        _uiState.update {
            it.copy(
                encoding = newEncoding,
                statusMessage = "Encoding changed to $newEncoding"
            )
        }
    }

    fun restoreCrashBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            val backupData = repository.loadCrashBackup()
            if (backupData != null) {
                val (contentString, fileName) = backupData
                _uiState.update {
                    it.copy(
                        fileName = fileName,
                        content = TextFieldValue(contentString),
                        isModified = true,
                        hasRecoveryBackup = false,
                        statusMessage = "Restored unsaved session work"
                    )
                }
            } else {
                _uiState.update { it.copy(hasRecoveryBackup = false) }
            }
            repository.clearCrashBackup()
        }
    }

    fun discardCrashBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearCrashBackup()
            _uiState.update {
                it.copy(
                    hasRecoveryBackup = false,
                    statusMessage = "Discarded session backup"
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    class Factory(private val repository: FileRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorViewModel(repository) as T
        }
    }
}
