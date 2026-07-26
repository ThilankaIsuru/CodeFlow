package com.example.codeflow.ui.versioning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.difflib.DiffUtils

/**
 * Line-by-line Diff Viewer Screen displaying visual comparison between historical
 * version text and current editor text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffViewerScreen(
    versionName: String,
    oldText: String,
    newText: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val diffItems = remember(oldText, newText) {
        computeDiffItems(oldText, newText)
    }

    val addedCount = remember(diffItems) { diffItems.count { it.type == DiffLineType.ADDED } }
    val deletedCount = remember(diffItems) { diffItems.count { it.type == DiffLineType.DELETED } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Diff: $versionName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "+$addedCount additions",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = "-$deletedCount deletions",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF44336)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Diff Viewer"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(diffItems) { item ->
                DiffLineRow(item = item)
            }
        }
    }
}

@Composable
private fun DiffLineRow(item: DiffLineItem) {
    val backgroundColor = when (item.type) {
        DiffLineType.ADDED -> Color(0x2200FF00)
        DiffLineType.DELETED -> Color(0x22FF0000)
        DiffLineType.UNCHANGED -> Color.Transparent
    }

    val textColor = when (item.type) {
        DiffLineType.ADDED -> Color(0xFF81C784)
        DiffLineType.DELETED -> Color(0xFFE57373)
        DiffLineType.UNCHANGED -> MaterialTheme.colorScheme.onSurface
    }

    val symbol = when (item.type) {
        DiffLineType.ADDED -> "+"
        DiffLineType.DELETED -> "-"
        DiffLineType.UNCHANGED -> " "
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 2.dp, horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Line Number Columns
            Text(
                text = item.oldLineNumber?.toString() ?: "",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.End,
                modifier = Modifier.width(28.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = item.newLineNumber?.toString() ?: "",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.End,
                modifier = Modifier.width(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))

            // Indicator symbol (+ / - / space)
            Text(
                text = symbol,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.width(16.dp)
            )

            // Line Text Content
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                color = textColor
            )
        }
    }
}

/**
 * Computes line-by-line diff items comparing [oldText] and [newText].
 */
private fun computeDiffItems(oldText: String, newText: String): List<DiffLineItem> {
    val oldLines = oldText.lines()
    val newLines = newText.lines()

    val patch = DiffUtils.diff(oldLines, newLines)
    val result = mutableListOf<DiffLineItem>()

    var oldIndex = 0
    var newIndex = 0

    val deltas = patch.deltas
    for (delta in deltas) {
        val source = delta.source
        val target = delta.target

        // Unchanged lines before this delta
        while (oldIndex < source.position && newIndex < target.position) {
            result.add(
                DiffLineItem(
                    oldLineNumber = oldIndex + 1,
                    newLineNumber = newIndex + 1,
                    type = DiffLineType.UNCHANGED,
                    text = oldLines[oldIndex]
                )
            )
            oldIndex++
            newIndex++
        }

        // Deleted lines from source
        for (i in 0 until source.lines.size) {
            result.add(
                DiffLineItem(
                    oldLineNumber = oldIndex + 1,
                    newLineNumber = null,
                    type = DiffLineType.DELETED,
                    text = source.lines[i]
                )
            )
            oldIndex++
        }

        // Added lines to target
        for (i in 0 until target.lines.size) {
            result.add(
                DiffLineItem(
                    oldLineNumber = null,
                    newLineNumber = newIndex + 1,
                    type = DiffLineType.ADDED,
                    text = target.lines[i]
                )
            )
            newIndex++
        }
    }

    // Remaining unchanged lines after all deltas
    while (oldIndex < oldLines.size || newIndex < newLines.size) {
        val oldNum = if (oldIndex < oldLines.size) oldIndex + 1 else null
        val newNum = if (newIndex < newLines.size) newIndex + 1 else null
        val lineText = if (oldIndex < oldLines.size) oldLines[oldIndex] else newLines[newIndex]

        result.add(
            DiffLineItem(
                oldLineNumber = oldNum,
                newLineNumber = newNum,
                type = DiffLineType.UNCHANGED,
                text = lineText
            )
        )
        if (oldIndex < oldLines.size) oldIndex++
        if (newIndex < newLines.size) newIndex++
    }

    return result
}
