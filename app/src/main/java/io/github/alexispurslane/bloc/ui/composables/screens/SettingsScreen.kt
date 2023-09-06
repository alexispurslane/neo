package io.github.alexispurslane.bloc.ui.composables.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.alexispurslane.bloc.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("BatteryLife")
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    ProvideTextStyle(value = MaterialTheme.typography.headlineLarge) {
                        Text("Settings")
                    }
                }
            )
        },
        content = {
            Column(
                modifier = Modifier
                    .padding(it)
                    .padding(horizontal = 15.dp)
                    .verticalScroll(
                        rememberScrollState()
                    ),
                verticalArrangement = Arrangement.spacedBy(
                    10.dp,
                    Alignment.CenterVertically
                ),
                horizontalAlignment = Alignment.Start
            ) {
                ProvideTextStyle(value = MaterialTheme.typography.headlineSmall) {
                    Text(
                        "APPEARANCE"
                    )
                }
                SettingsRow(
                    title = "Dark mode",
                    comment = "Use the dark theme for this app"
                ) {
                    Switch(checked = uiState.darkTheme, onCheckedChange = {
                        settingsViewModel.toggleDarkTheme(it)
                    })
                }

                Spacer(modifier = Modifier.height(20.dp))

                ProvideTextStyle(value = MaterialTheme.typography.headlineSmall) {
                    Text(
                        "NOTIFICATIONS"
                    )
                }
                val context = LocalContext.current
                val packageName = context.packageName
                val launchNotificationSettingsIntent =
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

                SettingsRow(
                    modifier = Modifier
                        .clickable {
                            context.startActivity(
                                launchNotificationSettingsIntent
                            )
                        },
                    title = "Notification settings",
                    comment = "Tap to customize how messages are presented"
                )
                SettingsRow(
                    title = "Notification service",
                    comment = "In order to receive notifications without using Google's servers, this service listens for new notifications over WebSockets. This is necessary to receive any notifications. It may use more battery."
                ) {
                    Switch(checked = uiState.serviceOn, onCheckedChange = {
                        settingsViewModel.toggleNotificationsService(it)
                    })
                }

                val launchPowerManagementSettingsIntent =
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

                SettingsRow(
                    modifier = Modifier
                        .clickable {
                            context.startActivity(
                                launchPowerManagementSettingsIntent
                            )
                        },
                    title = "Protect from doze",
                    comment = "In order to reliably deliver notifications, this app must be protected from power optimizations. Tap to go to those settings."
                )
            }
        },
        bottomBar = {}
    )
}

@Composable
fun SettingsRow(
    modifier: Modifier = Modifier,
    title: String,
    comment: String? = null,
    content: @Composable() (() -> Unit) = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(
                5.dp,
                Alignment.CenterVertically
            )
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.titleMedium) {
                Text(title)
            }
            if (comment != null) {
                ProvideTextStyle(value = MaterialTheme.typography.labelMedium) {
                    Text(comment)
                }
            }
        }
        content()
    }
}