package com.example.codeflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Developer Keyboard Toolbar pinned above soft keyboard via `imePadding`.
 * Provides quick insertion for symbols: `{`, `}`, `(`, `)`, `[`, `]`, `;`, `=`, `<`, `>`, `_`, `!`, `-`.
 */
@Composable
fun KeyboardToolbar(
    onSymbolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val developerSymbols = listOf(
        "{", "}", "(", ")", "[", "]", ";", "=", "<", ">", "_", "!", "-", "\"", "'", ":", ",", ".", "+", "*", "/", "&", "|", "\\", "#"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(developerSymbols) { symbol ->
                SymbolChip(
                    symbol = symbol,
                    onClick = { onSymbolClick(symbol) }
                )
            }
        }
    }
}

@Composable
private fun SymbolChip(
    symbol: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
