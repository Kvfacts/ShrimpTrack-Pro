package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.MainAquacultureApp
import com.example.ui.AquacultureViewModel
import com.example.ui.AquacultureViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Retain application context database reference safely
        val app = application as AquaShrimpApplication
        val repository = app.repository

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: AquacultureViewModel by viewModels {
                        AquacultureViewModelFactory(repository)
                    }
                    MainAquacultureApp(viewModel = viewModel)
                }
            }
        }
    }
}
