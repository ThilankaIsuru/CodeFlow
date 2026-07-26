package com.example.codeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.codeflow.ui.editor.EditorUiState

/**
 * Top App Bar displaying current file name, modified indicator, and action options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    uiState: EditorUiState,
    onMenuClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onSearchClick: () -> Unit,
    onNewClick: () -> Unit,
    onOpenClick: () -> Unit,
    onSaveClick: () -> Unit,
    onSaveAsClick: () -> Unit,
    onToggleEncodingClick: () -> Unit,
    onToggleReadOnlyClick: () -> Unit,
    onFormatCodeClick: () -> Unit,
    onVersionHistoryClick: () -> Unit,
    onToggleMarkdownPreviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open Navigation Drawer"
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (uiState.isModified) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.error,
                                shape = CircleShape
                            )
                    )
                }
            }
        },
        actions = {
            // Undo Action
            IconButton(
                onClick = onUndoClick,
                enabled = uiState.canUndo
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (uiState.canUndo) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }

            // Redo Action
            IconButton(
                onClick = onRedoClick,
                enabled = uiState.canRedo
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (uiState.canRedo) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }

            // Search Action
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }

            // Overflow Menu Icon
            Box {
                IconButton(onClick = { isMenuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options"
                    )
                }

                // Dropdown Menu
                DropdownMenu(
                    expanded = isMenuExpanded,
                    onDismissRequest = { isMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("New File") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            isMenuExpanded = false
                            onNewClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Open File") },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                        onClick = {
                            isMenuExpanded = false
                            onOpenClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Save") },
                        leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                        onClick = {
                            isMenuExpanded = false
                            onSaveClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Save As...") },
                        leadingIcon = { Icon(Icons.Default.SaveAs, contentDescription = null) },
                        onClick = {
                            isMenuExpanded = false
                            onSaveAsClick()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = { Text("Encoding: ${uiState.encoding}") },
                        leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                        onClick = {
                            isMenuExpanded = false
                            onToggleEncodingClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (uiState.isReadOnly) "Read-Only: On" else "Read-Only: Off")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.isReadOnly) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            isMenuExpanded = false
                            onToggleReadOnlyClick()
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DropdownMenuItem(
                        text = { Text("Format Code") },
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                        onClick = {
                            isMenuExpanded = false
                            onFormatCodeClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Version History") },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                        onClick = {
                            isMenuExpanded = false
                            onVersionHistoryClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (uiState.isPreviewMode) "Hide Preview" else "Toggle Markdown Preview")
                        },
                        leadingIcon = { Icon(Icons.Default.Preview, contentDescription = null) },
                        onClick = {
                            isMenuExpanded = false
                            onToggleMarkdownPreviewClick()
                        }
                    )
                }
            }
        }
    )
}
