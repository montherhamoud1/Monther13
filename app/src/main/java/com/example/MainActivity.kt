package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.shell.AppShell
import com.example.ui.shell.ShellViewModel

class MainActivity : ComponentActivity() {
    private val shellViewModel: ShellViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // RestoOps Enterprise - Phases 1 to 12 Build
        setContent {
            AppShell(viewModel = shellViewModel)
        }
    }
}
