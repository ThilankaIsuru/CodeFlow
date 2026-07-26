package com.example.codeflow.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Main Code Editor Area wrapper delegating to [CodeEditorEngine]
 * for real-time syntax highlighting, search term highlighting, and synced line numbering.
 */
@Composable
fun CodeEditorArea(
    content: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    isReadOnly: Boolean,
    wordWrap: Boolean,
    searchQuery: String = "",
    modifier: Modifier = Modifier
) {
    CodeEditorEngine(
        content = content,
        onContentChange = onContentChange,
        isReadOnly = isReadOnly,
        wordWrap = wordWrap,
        searchQuery = searchQuery,
        modifier = modifier
    )
}
