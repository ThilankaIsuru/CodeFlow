package com.example.codeflow.ui.editor

/**
 * In-memory Granular Undo/Redo Session Manager for text editor buffer states.
 * Manages character-state history stacks with capacity bounds.
 */
class UndoRedoManager(
    private val maxCapacity: Int = 50
) {
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    /**
     * Pushes a new meaningful text state onto the Undo stack and clears the Redo stack.
     */
    fun pushState(text: String) {
        val lastState = undoStack.lastOrNull()
        if (lastState == text) return // Avoid duplicate consecutive states

        if (undoStack.size >= maxCapacity) {
            undoStack.removeFirst()
        }
        undoStack.addLast(text)
        redoStack.clear()
    }

    /**
     * Performs Undo operation: pops the latest text state from Undo stack
     * and pushes the current state to Redo stack.
     */
    fun undo(currentText: String): String? {
        if (!canUndo) return null
        val previousState = undoStack.removeLast()
        if (redoStack.size >= maxCapacity) {
            redoStack.removeFirst()
        }
        redoStack.addLast(currentText)
        return previousState
    }

    /**
     * Performs Redo operation: pops the next text state from Redo stack
     * and pushes the current state to Undo stack.
     */
    fun redo(currentText: String): String? {
        if (!canRedo) return null
        val nextState = redoStack.removeLast()
        if (undoStack.size >= maxCapacity) {
            undoStack.removeFirst()
        }
        undoStack.addLast(currentText)
        return nextState
    }

    /**
     * Clears both Undo and Redo histories.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
