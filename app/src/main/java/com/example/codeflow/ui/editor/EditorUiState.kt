package com.example.codeflow.ui.editor

import androidx.compose.ui.text.input.TextFieldValue
import com.example.codeflow.data.local.entity.FileSnapshot

/**
 * Data representation of a recent file displayed in the navigation drawer.
 */
data class RecentFileItem(
    val id: Long,
    val fileName: String,
    val path: String,
    val relativeTimestamp: String
)

/**
 * UI State for the CodeFlow Editor Screen.
 */
data class EditorUiState(
    val fileName: String = "Untitled.kt",
    val absolutePath: String? = null,
    val activeFileId: Long? = null,
    val content: TextFieldValue = TextFieldValue(""),
    val isModified: Boolean = false,
    val isReadOnly: Boolean = false,
    val encoding: String = "UTF-8",
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val wordWrap: Boolean = false,
    val isPreviewMode: Boolean = false,
    val searchQuery: String = "",
    val replaceQuery: String = "",
    val isSearchActive: Boolean = false,
    val hasRecoveryBackup: Boolean = false,
    val statusMessage: String? = null,
    val recentFiles: List<RecentFileItem> = emptyList(),
    val shouldTriggerOpenPicker: Boolean = false,

    // Settings & Preference States
    val fontSizeSp: Int = 14,
    val showLineNumbers: Boolean = true,
    val isSettingsOpen: Boolean = false,

    // Phase 5 Versioning & Diff Viewer States
    val snapshots: List<FileSnapshot> = emptyList(),
    val isVersionHistoryOpen: Boolean = false,
    val isDiffViewerOpen: Boolean = false,
    val diffOldText: String? = null,
    val diffNewText: String? = null,
    val selectedVersionName: String? = null
)
