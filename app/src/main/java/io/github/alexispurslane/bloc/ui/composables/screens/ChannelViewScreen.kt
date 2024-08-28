package io.github.alexispurslane.bloc.ui.composables.screens

import android.content.ContentProvider
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.alexispurslane.bloc.LoadingScreen
import io.github.alexispurslane.bloc.R
import io.github.alexispurslane.bloc.ui.composables.misc.MessagesView
import io.github.alexispurslane.bloc.ui.composables.misc.launchActionWithAttachment
import io.github.alexispurslane.bloc.ui.composables.navigation.ChannelTopBar
import io.github.alexispurslane.bloc.ui.composables.navigation.MessageBar
import io.github.alexispurslane.bloc.viewmodels.ChannelViewModel

@Composable
fun ChannelViewScreen(
    navController: NavController,
    channelViewModel: ChannelViewModel = hiltViewModel()
) {
    val uiState by channelViewModel.uiState.collectAsState()

    if (uiState.channelInfo == null) {
        LoadingScreen()
    } else {
        Scaffold(
            topBar = {
                ChannelTopBar(uiState.channelInfo!!)
            },
            content = {
                if (uiState.error != null) {
                    val error = uiState.error!!.split(':')
                    ErrorScreen(title = error[0], message = error[1])
                } else {
                    MessagesView(
                        modifier = Modifier.padding(it),
                        uiState,
                        uiState.channelInfo!!,
                        onProfileClick = { userId ->
                            navController.navigate("profile/$userId")
                        }
                    )
                }
            },
            bottomBar = {
                val draftMessage by remember {
                    derivedStateOf { uiState.draftMessage }
                }
                var currentEmojiCompletions by remember { mutableStateOf(listOf<String>()) }

                LaunchedEffect(draftMessage) {
                    val prospectiveEmojiName = draftMessage.takeLastWhile { it != ':' }
                    val inEmoji = !prospectiveEmojiName.contains(' ') &&
                            draftMessage.count { it == ':' }.mod(2) != 0 &&
                            prospectiveEmojiName.length > 2
                    if (inEmoji) {
                        currentEmojiCompletions = listOf(prospectiveEmojiName)
                    } else {
                        currentEmojiCompletions = listOf()
                    }
                }

                Column {
                    if (currentEmojiCompletions.isNotEmpty()) {
                        CompletionDrawer {
                            currentEmojiCompletions.forEachIndexed { i, name ->
                                CompletionDrawerElement(
                                    i,
                                    onClick = { }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 5.dp)
                                            .size(24.dp)
                                            .align(Alignment.CenterVertically)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Custom emoji"
                                        )
                                    }

                                    Text(text = name)
                                }
                            }
                        }
                    } else if (uiState.files.isNotEmpty()) {
                        CompletionDrawer {
                            uiState.files.keys.forEachIndexed { i, name ->
                                CompletionDrawerElement(
                                    i,
                                    onClick = { channelViewModel.removeFiles(name) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 5.dp)
                                            .size(24.dp)
                                            .align(Alignment.CenterVertically)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Upload icon"
                                        )
                                    }

                                    Text(text = name)
                                }
                            }
                        }
                    }

                    MessageBar(
                        uiState.channelInfo?.name?.explicitName ?: "?",
                        uiState.draftMessage,
                        channelViewModel::updateMessage,
                        channelViewModel::sendMessage
                    )
                }
            }
        )
    }
    if (uiState.isSendError) {
        ErrorDialog(
            channelViewModel::onDialogDismiss,
            uiState.sendErrorTitle,
            uiState.sendErrorText
        )
    } else if (uiState.error != null) {
        ErrorDialog(
            channelViewModel::onDialogDismiss,
            "Uh oh!",
            uiState.error!!
        )
    }
}

@Composable
fun CompletionDrawerElement(index: Int, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    TextButton(
        modifier = Modifier
            .padding(
                start = 5.dp,
                end = 5.dp,
                bottom = 15.dp,
                top = if (index == 0) 15.dp else 0.dp
            )
            .height(38.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Color.Black),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            content = content
        )
    }
}

@Composable
fun CompletionDrawer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState())
            .background(DrawerDefaults.containerColor),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        content = content
    )
}
