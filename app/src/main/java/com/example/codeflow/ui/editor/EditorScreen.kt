package com.example.codeflow.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.codeflow.ui.components.CodeEditorArea
import com.example.codeflow.ui.components.KeyboardToolbar
import com.example.codeflow.ui.components.MarkdownPreview
import com.example.codeflow.ui.components.SearchBar
import com.example.codeflow.ui.components.SidebarDrawer
import com.example.codeflow.ui.components.TopBar
import com.example.codeflow.ui.dialogs.OpenFileDialog
import com.example.codeflow.ui.dialogs.SaveAsDialog
import com.example.codeflow.ui.theme.CodeFlowTheme
import com.example.codeflow.ui.versioning.DiffViewerScreen
import com.example.codeflow.ui.versioning.VersionHistoryDialog
import kotlinx.coroutines.launch

/**
 * CodeFlow Editor Screen bound to [EditorViewModel].
 * Integrates Android Native Storage Access Framework (SAF) document pickers for Save As and Open File,
 * Sidebar Drawer, Top Bar, Search & Replace Bar, Code Editor Area, Markdown Preview, Keyboard Toolbar,
 * crash recovery dialog, Version History sheet, and Diff Viewer screen.
 */
@Composable
fun EditorScreen(
    viewModel: EditorViewModel? = null,
    modifier: Modifier = Modifier
) {
    val uiState by if (viewModel != null) {
        viewModel.uiState.collectAsState()
    } else {
        remember { mutableStateOf(EditorUiState()) }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSaveAsDialogOpen by remember { mutableStateOf(false) }
    var isOpenFileDialogOpen by remember { mutableStateOf(false) }

    // Native Storage Access Framework (SAF) Launchers
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/*")
    ) { uri ->
        uri?.let {
            viewModel?.saveFileToUri(it.toString())
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel?.openFileFromUri(it.toString())
        }
    }

    val matchCount = remember(uiState.searchQuery, uiState.content.text) {
        if (uiState.searchQuery.isBlank()) 0
        else Regex(Regex.escape(uiState.searchQuery), RegexOption.IGNORE_CASE).findAll(uiState.content.text).count()
    }

    // Display status messages emitted by ViewModel
    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel?.clearStatusMessage()
        }
    }

    // 1. Crash Recovery Handshake Dialog
    if (uiState.hasRecoveryBackup) {
        AlertDialog(
            onDismissRequest = { viewModel?.discardCrashBackup() },
            title = {
                Text(
                    text = "Unsaved Work Detected",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "CodeFlow found an unsaved editing session from a previous crash. Would you like to restore your work?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel?.restoreCrashBackup() }
                ) {
                    Text("Restore Unsaved Work")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel?.discardCrashBackup() }
                ) {
                    Text("Discard Session Cache")
                }
            }
        )
    }

    // 2. Save As Dialog (Fallback manual path entry)
    if (isSaveAsDialogOpen) {
        SaveAsDialog(
            initialFileName = uiState.fileName,
            initialPath = uiState.absolutePath,
            onSaveConfirm = { path ->
                isSaveAsDialogOpen = false
                viewModel?.saveFileAs(path)
            },
            onDismissRequest = { isSaveAsDialogOpen = false }
        )
    }

    // 3. Open File Dialog (Fallback path entry / Recent file picker)
    if (isOpenFileDialogOpen) {
        OpenFileDialog(
            recentFiles = uiState.recentFiles,
            onOpenConfirm = { path ->
                isOpenFileDialogOpen = false
                viewModel?.openFile(path)
            },
            onDismissRequest = { isOpenFileDialogOpen = false }
        )
    }

    // 4. Version History Modal Bottom Sheet
    if (uiState.isVersionHistoryOpen) {
        VersionHistoryDialog(
            fileName = uiState.fileName,
            snapshots = uiState.snapshots,
            onCreateSnapshotClick = { name -> viewModel?.createVersionSnapshot(name) },
            onCompareDiffClick = { snapshot -> viewModel?.compareDiff(snapshot) },
            onRestoreVersionClick = { snapshot -> viewModel?.restoreVersion(snapshot) },
            onDismissRequest = { viewModel?.closeVersionHistory() }
        )
    }

    // 5. Line-by-Line Diff Viewer Overlay
    if (uiState.isDiffViewerOpen && uiState.diffOldText != null && uiState.diffNewText != null) {
        DiffViewerScreen(
            versionName = uiState.selectedVersionName ?: "Historical Version",
            oldText = uiState.diffOldText!!,
            newText = uiState.diffNewText!!,
            onClose = { viewModel?.closeDiffViewer() }
        )
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawer(
                recentFiles = uiState.recentFiles,
                activeFilePath = uiState.absolutePath,
                onFileSelect = { recentItem ->
                    coroutineScope.launch {
                        drawerState.close()
                        viewModel?.openFile(recentItem.path)
                    }
                },
                onNewFileClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        viewModel?.newFile()
                    }
                },
                onOpenFileClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        openDocumentLauncher.launch(arrayOf("*/*"))
                    }
                },
                onSettingsClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        snackbarHostState.showSnackbar("Settings opened")
                    }
                }
            )
        },
        modifier = modifier
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    uiState = uiState,
                    onMenuClick = {
                        coroutineScope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    },
                    onUndoClick = { viewModel?.undo() },
                    onRedoClick = { viewModel?.redo() },
                    onSearchClick = { viewModel?.toggleSearch() },
                    onNewClick = { viewModel?.newFile() },
                    onOpenClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                    onSaveClick = {
                        if (uiState.absolutePath == null) {
                            createDocumentLauncher.launch(uiState.fileName)
                        } else {
                            viewModel?.saveFile()
                        }
                    },
                    onSaveAsClick = { createDocumentLauncher.launch(uiState.fileName) },
                    onToggleEncodingClick = { viewModel?.toggleEncoding() },
                    onToggleReadOnlyClick = { viewModel?.toggleReadOnly() },
                    onFormatCodeClick = { viewModel?.formatCode() },
                    onVersionHistoryClick = { viewModel?.showVersionHistory() },
                    onToggleMarkdownPreviewClick = { viewModel?.toggleMarkdownPreview() }
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Interactive Search & Replace Bar Overlay
                if (uiState.isSearchActive) {
                    SearchBar(
                        query = uiState.searchQuery,
                        replaceQuery = uiState.replaceQuery,
                        matchCount = matchCount,
                        onQueryChange = { q -> viewModel?.onSearchQueryChange(q) },
                        onReplaceQueryChange = { rq -> viewModel?.onReplaceQueryChange(rq) },
                        onReplaceNextClick = { viewModel?.replaceNext() },
                        onReplaceAllClick = { viewModel?.replaceAll() },
                        onCloseClick = { viewModel?.closeSearch() }
                    )
                }

                // Main Code Workspace / Preview Area
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    if (uiState.isPreviewMode) {
                        MarkdownPreview(content = uiState.content.text)
                    } else {
                        CodeEditorArea(
                            content = uiState.content,
                            onContentChange = { newContent ->
                                viewModel?.onContentChange(newContent)
                            },
                            isReadOnly = uiState.isReadOnly,
                            wordWrap = uiState.wordWrap,
                            searchQuery = uiState.searchQuery
                        )
                    }
                }

                // Keyboard Accessory Toolbar (Pinned directly above soft keyboard)
                if (!uiState.isPreviewMode) {
                    KeyboardToolbar(
                        onSymbolClick = { symbol ->
                            viewModel?.insertSymbol(symbol)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditorScreenPreview() {
    CodeFlowTheme {
        EditorScreen()
    }
}
