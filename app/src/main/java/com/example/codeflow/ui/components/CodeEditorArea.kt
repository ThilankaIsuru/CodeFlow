package com.example.codeflow.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Main Code Editor Area wrapper delegating to [CodeEditorEngine]
 * for real-time Kotlin and Markdown syntax highlighting, search term highlighting, font scaling, and synced line numbering.
 */
@Composable
fun CodeEditorArea(
    content: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    isReadOnly: Boolean,
    wordWrap: Boolean,
    searchQuery: String = "",
    fileName: String = "Untitled.kt",
    fontSizeSp: Int = 14,
    showLineNumbers: Boolean = true,
    isDrawerOpen: Boolean = false,
    modifier: Modifier = Modifier
) {
    CodeEditorEngine(
        content = content,
        onContentChange = onContentChange,
        isReadOnly = isReadOnly,
        wordWrap = wordWrap,
        searchQuery = searchQuery,
        fileName = fileName,
        fontSizeSp = fontSizeSp,
        showLineNumbers = showLineNumbers,
        isDrawerOpen = isDrawerOpen,
        modifier = modifier
    )
}
