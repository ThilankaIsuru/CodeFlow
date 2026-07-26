package com.example.codeflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.codeflow.data.local.database.CodeFlowDatabase
import com.example.codeflow.data.repository.FileRepository
import com.example.codeflow.ui.editor.EditorScreen
import com.example.codeflow.ui.editor.EditorViewModel
import com.example.codeflow.ui.theme.CodeFlowTheme

class MainActivity : ComponentActivity() {

    private val viewModel: EditorViewModel by viewModels {
        val database = CodeFlowDatabase.getInstance(applicationContext)
        val repository = FileRepository(
            context = applicationContext,
            fileDao = database.fileDao(),
            snapshotDao = database.snapshotDao()
        )
        EditorViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CodeFlowTheme {
                EditorScreen(viewModel = viewModel)
            }
        }
    }
}