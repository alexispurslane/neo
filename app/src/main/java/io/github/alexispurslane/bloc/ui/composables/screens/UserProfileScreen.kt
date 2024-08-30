package io.github.alexispurslane.bloc.ui.composables.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.navigation.NavHostController
import io.github.alexispurslane.bloc.LoadingScreen
import io.github.alexispurslane.bloc.ui.composables.misc.UserCard
import io.github.alexispurslane.bloc.ui.composables.misc.UserRow
import io.github.alexispurslane.bloc.viewmodels.UserProfileViewModel
import kotlin.math.exp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    navController: NavHostController,
    userProfileViewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by userProfileViewModel.uiState.collectAsState()

    if (uiState.userProfile != null) {
        Column(
            modifier = Modifier.statusBarsPadding().navigationBarsPadding()
        ) {
            UserCard(userProfile = uiState.userProfile!!, client = uiState.client)
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
                if (uiState.isMyProfile) {
                    Text(
                        "appearance",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Black,
                        color = Color.DarkGray,
                        style = TextStyle(
                            fontFeatureSettings = "smcp"
                        )
                    )

                    SettingsRow(
                        title = "Message font size",
                        comment = "Set a custom font size for message text (UI text is unaffected)"
                    ) {
                        var expandedState by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expandedState, onExpandedChange = { expandedState = !expandedState }) {
                            TextField(
                                modifier = Modifier
                                    .menuAnchor(),
                                value = uiState.preferences["fontSize"] ?: "16.0",
                                onValueChange = {},
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedState) },
                                readOnly = true,
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(expanded = expandedState, onDismissRequest = { expandedState = false }) {
                                listOf(13.sp, 16.sp, 18.sp).forEach { size ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(text = "${size.value}")
                                        },
                                        onClick = {
                                            userProfileViewModel.setPreference("fontSize", size.value)
                                            expandedState = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    SettingsRow(
                        title = "Message text justification",
                        comment = "In case ragged edges bother you"
                    ) {
                        Switch(checked = uiState.preferences["justifyText"]?.toBooleanStrictOrNull() ?: true, onCheckedChange = {
                            userProfileViewModel.setPreference("justifyText", it)
                        })
                    }

                    SettingsRow(
                        title = "AMOLED Theme",
                        comment = "Pure black backgrounds"
                    ) {
                        Switch(checked = uiState.preferences["isAMOLED"]?.toBooleanStrictOrNull() ?: false, onCheckedChange = {
                            userProfileViewModel.setPreference("isAMOLED", it)
                        })
                    }

                    SettingsRow(
                        title = "Force Light Theme",
                        comment = "Overrides the default behavior of following your system theme."
                    ) {
                        Switch(checked = uiState.preferences["isLightOverride"]?.toBooleanStrictOrNull() ?: false, onCheckedChange = {
                            userProfileViewModel.setPreference("isLightOverride", it)
                        })
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "behavior",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Black,
                        color = Color.DarkGray,
                        style = TextStyle(
                            fontFeatureSettings = "smcp"
                        )
                    )

                    SettingsRow(
                        title = "Expand image attachments",
                        comment = "Whether image attachments are expanded or collapsed by default"
                    ) {
                        Switch(checked = uiState.preferences["expandImages"]?.toBooleanStrictOrNull() ?: true, onCheckedChange = {
                            userProfileViewModel.setPreference("expandImages", it)
                        })
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "notifications",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Black,
                        color = Color.DarkGray,
                        style = TextStyle(
                            fontFeatureSettings = "smcp"
                        )
                    )
                    val context = LocalContext.current
                    val packageName = context.packageName
                    val launchNotificationSettingsIntent =
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

                    SettingsRow(
                        modifier = Modifier
                            .clickable {
                                context.startActivity(launchNotificationSettingsIntent)
                            },
                        title = "Notification settings",
                        comment = "Tap to customize how messages are presented"
                    )
                    SettingsRow(
                        title = "Notification service",
                        comment = "Instead of using Google's servers to read your notifications, Bloc uses a background service."
                    ) {
                        Switch(checked = uiState.serviceOn, onCheckedChange = {
                            userProfileViewModel.toggleNotificationsService(it)
                        })
                    }

                    val launchPowerManagementSettingsIntent =
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

                    SettingsRow(
                        modifier = Modifier
                            .clickable {
                                context.startActivity(launchPowerManagementSettingsIntent)
                            },
                        title = "Protect from doze",
                        comment = "In order to reliably deliver notifications."
                    )
                    OutlinedButton(
                        modifier = Modifier
                            .height(50.dp)
                            .align(alignment = Alignment.CenterHorizontally),
                        onClick = {
                            userProfileViewModel.logout()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Log Out")
                    }
                }
            }
        }
    } else {
        LoadingScreen()
    }
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
            Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp)
            if (comment != null)
                Text(comment, color = Color.Gray, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier.weight(0.4f).padding(start = 5.dp)
        ) {
            content()
        }
    }
}