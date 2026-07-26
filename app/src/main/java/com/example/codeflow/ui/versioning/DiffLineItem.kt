package com.example.codeflow.ui.versioning

/**
 * Structural buckets for line-by-line diff operations.
 */
enum class DiffLineType {
    UNCHANGED,
    ADDED,
    DELETED
}

/**
 * Data model representing a line in the Diff Viewer UI.
 */
data class DiffLineItem(
    val oldLineNumber: Int?,
    val newLineNumber: Int?,
    val type: DiffLineType,
    val text: String
)
