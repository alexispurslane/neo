package io.github.alexispurslane.bloc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.alexispurslane.bloc.ui.theme.BlocTheme
import io.github.alexispurslane.service.Actions
import io.github.alexispurslane.service.ServiceState
import io.github.alexispurslane.service.WebSocketNotificationService
import io.github.alexispurslane.service.getServiceState

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (getServiceState(this) == ServiceState.STOPPED) {
            Intent(this, WebSocketNotificationService::class.java).apply {
                action = Actions.START.name
                startForegroundService(this)
            }
        }

        setContent {
            BlocTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}
