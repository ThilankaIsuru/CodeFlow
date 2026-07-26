package com.example.codeflow.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.codeflow.ui.editor.RecentFileItem

/**
 * Sidebar Navigation Drawer Component for CodeFlow.
 * Includes Header, Recent Files list, and Footer Actions.
 */
@Composable
fun SidebarDrawer(
    recentFiles: List<RecentFileItem>,
    activeFilePath: String?,
    onFileSelect: (RecentFileItem) -> Unit,
    onNewFileClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier.width(320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier.fillMaxHeight()
        ) {
            // 1. Header with custom Logo & Branding
            DrawerHeader()

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // 2. Recent Files Section Header & List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT FILES",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(2.dp)
                    ) {
                        Text(
                            text = "${recentFiles.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(recentFiles, key = { it.id }) { item ->
                        val isSelected = item.path == activeFilePath
                        RecentFileListItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = { onFileSelect(item) }
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // 3. Footer Actions
            DrawerFooter(
                onNewFileClick = onNewFileClick,
                onOpenFileClick = onOpenFileClick,
                onSettingsClick = onSettingsClick
            )
        }
    }
}

/**
 * Custom Header displaying CodeFlow logo graphic and title.
 */
@Composable
private fun DrawerHeader() {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(brush = gradientBrush)
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Custom Quill & Code Bracket Canvas Logo
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                CodeQuillLogoGraphic(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "CodeFlow",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Delta Versioned Editor",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Stylized Canvas Graphic representing code brackets `{ }` with a quill stroke.
 */
@Composable
private fun CodeQuillLogoGraphic(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val w = size.width
        val h = size.height

        // Left Code Bracket `<`
        val leftPath = Path().apply {
            moveTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.15f, h * 0.50f)
            lineTo(w * 0.35f, h * 0.75f)
        }
        drawPath(
            path = leftPath,
            color = color,
            style = Stroke(width = strokeWidth)
        )

        // Right Code Bracket `>`
        val rightPath = Path().apply {
            moveTo(w * 0.65f, h * 0.25f)
            lineTo(w * 0.85f, h * 0.50f)
            lineTo(w * 0.65f, h * 0.75f)
        }
        drawPath(
            path = rightPath,
            color = color,
            style = Stroke(width = strokeWidth)
        )

        // Quill Pen / Slash Line `/`
        val quillPath = Path().apply {
            moveTo(w * 0.60f, h * 0.20f)
            lineTo(w * 0.40f, h * 0.80f)
        }
        drawPath(
            path = quillPath,
            color = color.copy(alpha = 0.9f),
            style = Stroke(width = strokeWidth + 1.dp.toPx())
        )
    }
}

/**
 * Recent file list item component.
 */
@Composable
private fun RecentFileListItem(
    item: RecentFileItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "fileItemBg"
    )

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.fileName.endsWith(".kt")) Icons.Default.Code else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = if (item.fileName.contains(".")) FontFamily.Monospace else FontFamily.Default
                )
                Text(
                    text = item.relativeTimestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Drawer Footer containing New File, Open File, and Settings options.
 */
@Composable
private fun DrawerFooter(
    onNewFileClick: () -> Unit,
    onOpenFileClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FooterActionButton(
            label = "New File",
            icon = Icons.Default.Add,
            onClick = onNewFileClick
        )
        FooterActionButton(
            label = "Open File",
            icon = Icons.Default.FolderOpen,
            onClick = onOpenFileClick
        )
        FooterActionButton(
            label = "Settings",
            icon = Icons.Default.Settings,
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun FooterActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
