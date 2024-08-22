package io.github.alexispurslane.bloc

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.alexispurslane.bloc.ui.theme.BlocTheme
import io.github.alexispurslane.service.Actions
import io.github.alexispurslane.service.NotificationService
import io.github.alexispurslane.service.ServiceState
import io.github.alexispurslane.service.getServiceState

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )
        if (permissions.contains(Manifest.permission.FOREGROUND_SERVICE)) {
            if (getServiceState(this) == ServiceState.STOPPED) {
                Intent(this, NotificationService::class.java).apply {
                    action = Actions.START.name
                    startForegroundService(this)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val context = this as Context

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.FOREGROUND_SERVICE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Manifest.permission.RECEIVE_BOOT_COMPLETED
                ),
                0
            )
        } else {
            if (getServiceState(this) == ServiceState.STOPPED) {
                Intent(this, NotificationService::class.java).apply {
                    action = Actions.START.name
                    startForegroundService(this)
                }
            }
        }

        // If we're on Android 33 or later, we need to request notifications permission!
        if (Build.VERSION.SDK_INT >= 33) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
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
