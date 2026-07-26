package com.example.codeflow.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Custom [VisualTransformation] providing real-time Kotlin syntax highlighting and search query highlighting.
 * Uses pre-compiled regex patterns and returning [OffsetMapping.Identity] for low overhead.
 */
class KotlinSyntaxHighlighter(
    private val searchQuery: String = ""
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        val builder = AnnotatedString.Builder(rawText)

        val commentRanges = COMMENT_REGEX.findAll(rawText).map { it.range }.toList()
        val stringRanges = STRING_REGEX.findAll(rawText).map { it.range }.toList()
        val excluded = commentRanges + stringRanges

        // 1. Highlight Comments (Single-line and Multi-line)
        COMMENT_REGEX.findAll(rawText).forEach { match ->
            builder.addStyle(COMMENT_STYLE, match.range.first, match.range.last + 1)
        }

        // 2. Highlight Strings and Char Literals
        STRING_REGEX.findAll(rawText).forEach { match ->
            if (!isPositionInRanges(match.range.first, commentRanges)) {
                builder.addStyle(STRING_STYLE, match.range.first, match.range.last + 1)
            }
        }

        // 3. Highlight Annotations
        ANNOTATION_REGEX.findAll(rawText).forEach { match ->
            if (!isPositionInRanges(match.range.first, excluded)) {
                builder.addStyle(ANNOTATION_STYLE, match.range.first, match.range.last + 1)
            }
        }

        // 4. Highlight Numbers
        NUMBER_REGEX.findAll(rawText).forEach { match ->
            if (!isPositionInRanges(match.range.first, excluded)) {
                builder.addStyle(NUMBER_STYLE, match.range.first, match.range.last + 1)
            }
        }

        // 5. Highlight Keywords
        KEYWORD_REGEX.findAll(rawText).forEach { match ->
            if (!isPositionInRanges(match.range.first, excluded)) {
                builder.addStyle(KEYWORD_STYLE, match.range.first, match.range.last + 1)
            }
        }

        // 6. Highlight Search Query Matches
        if (searchQuery.isNotBlank()) {
            var startIndex = 0
            while (startIndex < rawText.length) {
                val index = rawText.indexOf(searchQuery, startIndex, ignoreCase = true)
                if (index == -1) break
                val endIndex = index + searchQuery.length
                builder.addStyle(SEARCH_HIGHLIGHT_STYLE, index, endIndex)
                startIndex = index + maxOf(1, searchQuery.length)
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun isPositionInRanges(position: Int, ranges: Iterable<IntRange>): Boolean {
        return ranges.any { position in it }
    }

    companion object {
        // Dark Mode Syntax Highlight Palette
        val KEYWORD_STYLE = SpanStyle(
            color = Color(0xFFFF79C6),
            fontWeight = FontWeight.Bold
        )
        val STRING_STYLE = SpanStyle(
            color = Color(0xFF50FA7B)
        )
        val COMMENT_STYLE = SpanStyle(
            color = Color(0xFF6272A4)
        )
        val ANNOTATION_STYLE = SpanStyle(
            color = Color(0xFFFFB86C),
            fontWeight = FontWeight.SemiBold
        )
        val NUMBER_STYLE = SpanStyle(
            color = Color(0xFF8BE9FD)
        )
        val SEARCH_HIGHLIGHT_STYLE = SpanStyle(
            background = Color(0xFFFBC02D),
            color = Color(0xFF000000),
            fontWeight = FontWeight.Bold
        )

        private const val KOTLIN_KEYWORDS =
            "val|var|fun|class|object|interface|return|if|else|for|while|import|package|" +
            "true|false|null|private|public|protected|internal|override|suspend|data|init"

        private val KEYWORD_REGEX = Regex("\\b($KOTLIN_KEYWORDS)\\b")
        private val STRING_REGEX = Regex("\"\"\"[\\s\\S]*?\"\"\"|\".*?\"|'.*?'")
        private val COMMENT_REGEX = Regex("//.*|/\\*[\\s\\S]*?\\*/")
        private val ANNOTATION_REGEX = Regex("@[a-zA-Z0-9_]+")
        private val NUMBER_REGEX = Regex("\\b(0x[0-9a-fA-F]+|0b[01]+|\\d+(\\.\\d+)?([fFL])?)\\b")
    }
}
