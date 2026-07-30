package com.example.codeflow.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Unified [VisualTransformation] providing real-time syntax highlighting for Kotlin (.kt)
 * and Markdown (.md / .markdown) files, as well as search match highlighting.
 */
class CodeSyntaxHighlighter(
    private val fileName: String = "Untitled.kt",
    private val searchQuery: String = ""
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val rawText = text.text
        val builder = AnnotatedString.Builder(rawText)

        val isMarkdown = fileName.endsWith(".md", ignoreCase = true) || fileName.endsWith(".markdown", ignoreCase = true)

        if (isMarkdown) {
            highlightMarkdown(rawText, builder)
        } else {
            highlightKotlin(rawText, builder)
        }

        // Search Query Highlighting (Common to Kotlin & Markdown)
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

    private fun highlightMarkdown(rawText: String, builder: AnnotatedString.Builder) {
        // 1. Headers (# H1, ## H2, ### H3...)
        MD_HEADER_REGEX.findAll(rawText).forEach { match ->
            builder.addStyle(MD_HEADER_STYLE, match.range.first, match.range.last + 1)
        }

        // 2. Fenced Code Blocks (```code```)
        MD_CODE_BLOCK_REGEX.findAll(rawText).forEach { match ->
            builder.addStyle(MD_CODE_STYLE, match.range.first, match.range.last + 1)
        }

        // 3. Inline Code (`code`)
        MD_INLINE_CODE_REGEX.findAll(rawText).forEach { match ->
            builder.addStyle(MD_CODE_STYLE, match.range.first, match.range.last + 1)
        }

        // 4. Bold Text (**text** or __text__)
        MD_BOLD_REGEX.findAll(rawText).forEach { match ->
            builder.addStyle(MD_BOLD_STYLE, match.range.first, match.range.last + 1)
        }

        // 5. Italic Text (*text* or _text_)
        MD_ITALIC_REGEX.findAll(rawText).forEach { match ->
            builder.addStyle(MD_ITALIC_STYLE, match.range.first, match.range.last + 1)
        }

        // 6. Markdown Links ([title](url))
        MD_LINK_REGEX.findAll(rawText).forEach { match ->
            builder.addStyle(MD_LINK_STYLE, match.range.first, match.range.last + 1)
        }

        // 7. Blockquotes (> quote)
        MD_QUOTE_REGEX.findAll(rawText).forEach { match ->
            builder.addStyle(MD_QUOTE_STYLE, match.range.first, match.range.last + 1)
        }
    }

    private fun highlightKotlin(rawText: String, builder: AnnotatedString.Builder) {
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
    }

    private fun isPositionInRanges(position: Int, ranges: Iterable<IntRange>): Boolean {
        return ranges.any { position in it }
    }

    companion object {
        // --- KOTLIN STYLES ---
        val KEYWORD_STYLE = SpanStyle(color = Color(0xFFFF79C6), fontWeight = FontWeight.Bold)
        val STRING_STYLE = SpanStyle(color = Color(0xFF50FA7B))
        val COMMENT_STYLE = SpanStyle(color = Color(0xFF6272A4))
        val ANNOTATION_STYLE = SpanStyle(color = Color(0xFFFFB86C), fontWeight = FontWeight.SemiBold)
        val NUMBER_STYLE = SpanStyle(color = Color(0xFF8BE9FD))

        // --- MARKDOWN STYLES ---
        val MD_HEADER_STYLE = SpanStyle(color = Color(0xFF80DEEA), fontWeight = FontWeight.Bold)
        val MD_CODE_STYLE = SpanStyle(color = Color(0xFF80CBC4), background = Color(0xFF1E262C))
        val MD_BOLD_STYLE = SpanStyle(color = Color(0xFFFFF59D), fontWeight = FontWeight.Bold)
        val MD_ITALIC_STYLE = SpanStyle(color = Color(0xFFF48FB1), fontStyle = FontStyle.Italic)
        val MD_LINK_STYLE = SpanStyle(color = Color(0xFF90CAF9))
        val MD_QUOTE_STYLE = SpanStyle(color = Color(0xFFA5D6A7), fontStyle = FontStyle.Italic)

        // --- SEARCH HIGHLIGHT ---
        val SEARCH_HIGHLIGHT_STYLE = SpanStyle(background = Color(0xFFFBC02D), color = Color(0xFF000000), fontWeight = FontWeight.Bold)

        // --- REGEX PATTERNS ---
        private const val KOTLIN_KEYWORDS =
            "val|var|fun|class|object|interface|return|if|else|for|while|import|package|" +
            "true|false|null|private|public|protected|internal|override|suspend|data|init"

        private val KEYWORD_REGEX = Regex("\\b($KOTLIN_KEYWORDS)\\b")
        private val STRING_REGEX = Regex("\"\"\"[\\s\\S]*?\"\"\"|\".*?\"|'.*?'")
        private val COMMENT_REGEX = Regex("//.*|/\\*[\\s\\S]*?\\*/")
        private val ANNOTATION_REGEX = Regex("@[a-zA-Z0-9_]+")
        private val NUMBER_REGEX = Regex("\\b(0x[0-9a-fA-F]+|0b[01]+|\\d+(\\.\\d+)?([fFL])?)\\b")

        private val MD_HEADER_REGEX = Regex("^#{1,6}\\s+.*", RegexOption.MULTILINE)
        private val MD_CODE_BLOCK_REGEX = Regex("```[\\s\\S]*?```")
        private val MD_INLINE_CODE_REGEX = Regex("`[^`\\n]+`")
        private val MD_BOLD_REGEX = Regex("\\*\\*.*?\\*\\*|__.*?__")
        private val MD_ITALIC_REGEX = Regex("(?<!\\*)\\*[^\\*\\n]+\\*(?!\\*)|(?<!_)_[^_\\n]+_(?!_)")
        private val MD_LINK_REGEX = Regex("\\[.*?\\]\\(.*?\\)")
        private val MD_QUOTE_REGEX = Regex("^>.*", RegexOption.MULTILINE)
    }
}
