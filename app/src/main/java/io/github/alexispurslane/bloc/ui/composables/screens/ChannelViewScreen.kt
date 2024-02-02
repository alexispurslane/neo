package io.github.alexispurslane.bloc.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.alexispurslane.bloc.LoadingScreen
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.ui.composables.misc.MessagesView
import io.github.alexispurslane.bloc.ui.composables.misc.UserRow
import io.github.alexispurslane.bloc.ui.composables.navigation.ChannelTopBar
import io.github.alexispurslane.bloc.ui.composables.navigation.MessageBar
import io.github.alexispurslane.bloc.viewmodels.ChannelPulloutViewModel
import io.github.alexispurslane.bloc.viewmodels.ServerChannelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelViewScreen(
    navController: NavController,
    channelViewModel: ServerChannelViewModel = hiltViewModel()
) {
    val uiState by channelViewModel.uiState.collectAsState()

    if (uiState.channelInfo == null) {
        LoadingScreen()
    } else {
        if (uiState.channelInfo is RevoltChannel.TextChannel) {
            val channelInfo = uiState.channelInfo as RevoltChannel.TextChannel
            Scaffold(
                topBar = {
                    ChannelTopBar(channelInfo)
                },
                content = {
                    if (uiState.error != null) {
                        val error = uiState.error!!.split(':')
                        ErrorScreen(title = error[0], message = error[1])
                    } else {
                        MessagesView(
                            modifier = Modifier.padding(it),
                            uiState,
                            channelInfo,
                            onProfileClick = { userId ->
                                navController.navigate("profile/$userId")
                            },
                            onMessageClick = { messageId, emojiId, isUsersReaction ->
                                if (emojiId == null) {
                                    channelViewModel.handleMessageClick(
                                        messageId
                                    )
                                } else {
                                    channelViewModel.handleReactClick(
                                        messageId,
                                        emojiId,
                                        isUsersReaction
                                    )
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    MessageBar(
                        channelInfo.name,
                        uiState.draftMessage,
                        uiState.serverEmoji,
                        channelViewModel::updateMessage,
                        channelViewModel::sendMessage,
                    )
                }
            )
        }
    }

    if (uiState.selectedMessage != null) {
        val bottomSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )
        ModalBottomSheet(
            sheetState = bottomSheetState,
            onDismissRequest = { channelViewModel.handleMessageClose() }
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TextButton(
                    modifier = Modifier
                        .height(70.dp)
                        .fillMaxWidth(),
                    onClick = { /*TODO*/ }
                ) {
                    Text("Add Reaction")
                }

                if (uiState.selectedMessage?.authorId == uiState.currentUserId) {
                    TextButton(
                        modifier = Modifier
                            .height(70.dp)
                            .fillMaxWidth(),
                        onClick = { channelViewModel.handleDeleteMessage() }
                    ) {
                        Text("Delete Message")
                    }
                    TextButton(
                        modifier = Modifier
                            .height(70.dp)
                            .fillMaxWidth(),
                        onClick = { channelViewModel.handleEditMessage() }
                    ) {
                        Text("Edit Message")
                    }
                } else {
                    TextButton(
                        modifier = Modifier
                            .height(70.dp)
                            .fillMaxWidth(),
                        onClick = { /*TODO*/ }
                    ) {
                        Text("Report Message")
                    }
                }
            }
        }
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
fun ChannelViewPullout(
    navController: NavController,
    currentChannelId: String?,
    channelPulloutViewModel: ChannelPulloutViewModel = hiltViewModel()
) {
    val uiState by channelPulloutViewModel.uiState.collectAsState()
    LaunchedEffect(currentChannelId) {
        if (currentChannelId != null)
            channelPulloutViewModel.setChannelId(currentChannelId)
    }

    val channelInfo by uiState.channelInfo
    val serverInfo by uiState.serverInfo
    if (channelInfo != null && serverInfo != null) {
        when (val channel = channelInfo!!) {
            is RevoltChannel.TextChannel -> {
                Column(
                    modifier = Modifier.padding(horizontal = 15.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth()
                            .background(Color(0x22000000)),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        ProvideTextStyle(value = MaterialTheme.typography.headlineLarge) {
                            Text(
                                "#${(channel.name)}",
                                modifier = Modifier.padding(
                                    bottom = 15.dp
                                ),
                                textAlign = TextAlign.Start,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        ProvideTextStyle(value = MaterialTheme.typography.labelMedium) {
                            Text(channel.description ?: "A text channel.")
                        }
                    }
                    for ((user, member) in uiState.users) {
                        UserRow(
                            modifier = Modifier.padding(top = 10.dp),
                            userProfile = user.copy(
                                displayName = member.nickname
                                    ?: user.displayName,
                                avatar = member.avatar ?: user.avatar
                            ),
                            showFullInfo = false,
                            iconSize = 40.dp,
                            onClick = { userId ->
                                navController.navigate("profile/$userId")
                            }
                        )
                    }
                }
            }

            else -> {}
        }
    }
}