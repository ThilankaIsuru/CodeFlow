package com.example.codeflow.data.repository

import com.example.codeflow.data.local.entity.FileSnapshot
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.PatchFailedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Engine for calculating, generating, and applying incremental unified text deltas
 * using 'java-diff-utils'.
 */
object DeltaSnapshotEngine {

    /**
     * Computes unified diff patch string between [oldContent] and [newContent].
     */
    suspend fun computeUnifiedDiff(
        fileName: String,
        oldContent: String,
        newContent: String
    ): String = withContext(Dispatchers.Default) {
        val oldLines = oldContent.lines()
        val newLines = newContent.lines()

        val patch = DiffUtils.diff(oldLines, newLines)
        val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(
            fileName,
            fileName,
            oldLines,
            patch,
            3
        )
        unifiedDiff.joinToString("\n")
    }

    /**
     * Applies unified diff patch string onto [baseContent].
     */
    suspend fun applyUnifiedDiff(
        baseContent: String,
        diffPatch: String
    ): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            if (diffPatch.isBlank()) return@runCatching baseContent

            val baseLines = baseContent.lines()
            val patchLines = diffPatch.lines()
            val parsedPatch = UnifiedDiffUtils.parseUnifiedDiff(patchLines)

            try {
                val patchedLines = DiffUtils.patch(baseLines, parsedPatch)
                patchedLines.joinToString("\n")
            } catch (e: PatchFailedException) {
                if (baseContent.isEmpty()) {
                    // Fallback for line-based extraction if base content was empty
                    patchLines.filterNot {
                        it.startsWith("---") || it.startsWith("+++") || it.startsWith("@@") || it.startsWith("Index:")
                    }.map {
                        if (it.startsWith("+")) it.substring(1) else it
                    }.joinToString("\n")
                } else {
                    throw IllegalStateException("Failed to apply version patch: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Reconstructs full document string at [targetVersion] starting from empty string baseline.
     */
    suspend fun reconstructVersion(
        snapshots: List<FileSnapshot>,
        targetVersion: Int
    ): Result<String> = withContext(Dispatchers.Default) {
        runCatching {
            val orderedSnapshots = snapshots.filter { it.versionNumber <= targetVersion }
                .sortedBy { it.versionNumber }

            var currentText = ""
            for (snapshot in orderedSnapshots) {
                val patchResult = applyUnifiedDiff(currentText, snapshot.deltaPatch)
                currentText = patchResult.getOrDefault(currentText)
            }
            currentText
        }
    }
}
