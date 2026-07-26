package com.example.codeflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Interactive Search and Replace Bar component.
 * Supports real-time query matching, Replace Next, and Replace All actions.
 */
@Composable
fun SearchBar(
    query: String,
    replaceQuery: String,
    matchCount: Int,
    onQueryChange: (String) -> Unit,
    onReplaceQueryChange: (String) -> Unit,
    onReplaceNextClick: () -> Unit,
    onReplaceAllClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showReplacePanel by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // --- Row 1: Search Query Row ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Find...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (query.isNotBlank()) {
                Text(
                    text = if (matchCount == 1) "1 match" else "$matchCount matches",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (matchCount > 0) Color(0xFFFBC02D) else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            IconButton(onClick = { showReplacePanel = !showReplacePanel }) {
                Icon(
                    imageVector = Icons.Default.FindReplace,
                    contentDescription = "Toggle Replace panel",
                    tint = if (showReplacePanel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onCloseClick) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close search bar")
            }
        }

        // --- Row 2: Replace Query & Actions Panel ---
        AnimatedVisibility(visible = showReplacePanel) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = replaceQuery,
                        onValueChange = onReplaceQueryChange,
                        placeholder = { Text("Replace with...") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = onReplaceNextClick,
                        enabled = query.isNotBlank() && matchCount > 0,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Replace")
                    }

                    Button(
                        onClick = onReplaceAllClick,
                        enabled = query.isNotBlank() && matchCount > 0,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Replace All")
                    }
                }
            }
        }
    }
}
