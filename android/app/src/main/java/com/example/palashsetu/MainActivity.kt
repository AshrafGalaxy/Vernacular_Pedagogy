package com.example.palashsetu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.palashsetu.data.local.FlnRepository
import com.example.palashsetu.theme.PalashSetuTheme
import com.example.palashsetu.ui.main.MainAppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Tier-1 Room/SQLite Curriculum Cache
        FlnRepository.initializeFromAssets(applicationContext)

        enableEdgeToEdge()
        setContent {
            PalashSetuTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContainer()
                }
            }
        }
    }
}
