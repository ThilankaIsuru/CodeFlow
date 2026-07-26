package com.example.codeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.codeflow.ui.editor.KotlinSyntaxHighlighter

/**
 * Editor Engine Composable unifying dynamic Kotlin syntax highlighting
 * with a synced line numbering gutter that correctly handles wrapped lines.
 */
@Composable
fun CodeEditorEngine(
    content: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    isReadOnly: Boolean,
    wordWrap: Boolean,
    searchQuery: String = "",
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val highlighter = remember(searchQuery) { KotlinSyntaxHighlighter(searchQuery) }

    // Calculate line numbers per visual row based on TextLayoutResult
    val lineLabels = remember(textLayoutResult, content.text) {
        val layout = textLayoutResult
        if (layout == null) {
            // Fallback before initial layout pass
            val totalLines = maxOf(1, content.text.count { it == '\n' } + 1)
            (1..totalLines).map { it.toString() }
        } else {
            val visualLineCount = layout.lineCount
            var logicalLineIndex = 1
            val labels = ArrayList<String>(visualLineCount)

            for (i in 0 until visualLineCount) {
                val startOffset = layout.getLineStart(i)
                val isLogicalLineStart = i == 0 || (startOffset > 0 && content.text.getOrNull(startOffset - 1) == '\n')

                if (isLogicalLineStart) {
                    labels.add(logicalLineIndex.toString())
                    logicalLineIndex++
                } else {
                    labels.add("") // Wrapped line continuation
                }
            }
            labels
        }
    }

    val fontSize = 14.sp
    val lineHeight = 20.sp

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 1. Line Numbering Gutter (Synced with editor scroll)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .verticalScroll(verticalScrollState)
        ) {
            Text(
                text = lineLabels.joinToString("\n"),
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.End
                ),
                modifier = Modifier.width(36.dp)
            )
        }

        // Solid Vertical Separator Border
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        )

        // 2. Main Editor Canvas with Syntax Highlighting & Layout Tracker
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(12.dp)
                .verticalScroll(verticalScrollState)
                .then(
                    if (!wordWrap) Modifier.horizontalScroll(horizontalScrollState) else Modifier
                )
        ) {
            BasicTextField(
                value = content,
                onValueChange = { if (!isReadOnly) onContentChange(it) },
                readOnly = isReadOnly,
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                visualTransformation = highlighter,
                onTextLayout = { textLayoutResult = it },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxHeight()
                    .then(
                        if (wordWrap) Modifier.fillMaxWidth() else Modifier
                    )
            )
        }
    }
}
