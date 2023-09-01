package io.github.alexispurslane.bloc.ui.composables.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.alexispurslane.bloc.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    Column(
        modifier = Modifier
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
        Box(
            modifier = Modifier
                .height(100.dp)
                .padding(bottom = 30.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                "Settings",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Start,
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

        val context = LocalContext.current
        val packageName = context.packageName
        val launchNotificationSettingsIntent =
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    context.startActivity(launchNotificationSettingsIntent)
                }
        ) {
            SettingsRow(
                title = "Notification settings",
                comment = "Tap to customize how messages are presented"
            ) {

            }
        }
        SettingsRow(
            title = "Notification service",
            comment = "This service listens over WebSockets for new notifications. It may use more battery."
        ) {
            Switch(checked = uiState.serviceOn, onCheckedChange = {
                settingsViewModel.toggleNotificationsService(it)
            })
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    comment: String? = null,
    content: @Composable() (() -> Unit)
) {
    Row(
        modifier = Modifier
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
            Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp)
            if (comment != null)
                Text(comment, color = Color.Gray, fontSize = 15.sp)
        }
        content()
    }
}