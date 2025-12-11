package com.seenubommisetti.resq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.seenubommisetti.resq.screens.ResQAppScreen
import com.seenubommisetti.resq.ui.theme.ResQTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Scaffold { innerPadding ->
                ResQTheme {
                    ResQAppScreen(modifier = Modifier.padding(innerPadding))
                }

            }
        }
    }
}

