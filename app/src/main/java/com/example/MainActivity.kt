package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SleepAppNavigation
import com.example.ui.SleepReceiver
import com.example.ui.SleepViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {

    private var currentViewModel: SleepViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: SleepViewModel = viewModel()
            currentViewModel = viewModel

            // Read the dynamic theme state
            val selectedPreset by viewModel.themePreset.collectAsState()
            val isDark by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(
                themePreset = selectedPreset,
                darkTheme = isDark
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    SleepAppNavigation(viewModel = viewModel)
                }
            }
        }

        // Handle initial intent if app was launched via alarm notification
        handleIncomingSessionIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingSessionIntent(intent)
    }

    private fun handleIncomingSessionIntent(intent: Intent?) {
        val action = intent?.action ?: return
        if (action == SleepReceiver.ACTION_WAKEUP_OPEN || action == SleepReceiver.ACTION_START_NOW) {
            val sessionId = intent.getLongExtra("EXTRA_SESSION_ID", -1L)
            if (sessionId != -1L) {
                // Let ViewModel trigger audio alerts and visual modal overlay
                currentViewModel?.handleIncomingAlarm(sessionId)
            }
        }
    }
}
