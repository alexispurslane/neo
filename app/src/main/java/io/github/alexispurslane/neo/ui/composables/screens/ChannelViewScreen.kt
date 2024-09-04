package io.github.alexispurslane.neo.ui.composables.screens

import android.os.Message
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.DrawerDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.alexispurslane.neo.LoadingScreen
import io.github.alexispurslane.neo.R
import io.github.alexispurslane.neo.ui.composables.misc.ErrorDialog
import io.github.alexispurslane.neo.ui.composables.misc.MessageBody
import io.github.alexispurslane.neo.ui.composables.misc.MessageContent
import io.github.alexispurslane.neo.ui.composables.misc.MessagesView
import io.github.alexispurslane.neo.ui.composables.navigation.ChannelTopBar
import io.github.alexispurslane.neo.ui.composables.navigation.MessageBar
import io.github.alexispurslane.neo.viewmodels.ChannelViewModel
import io.github.alexispurslane.neo.viewmodels.MessageResponse
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent

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
                    val messages by uiState.messages!!.collectAsState()
                    MessagesView(
                        modifier = Modifier.padding(it),
                        uiState,
                        uiState.channelInfo!!,
                        messages,
                        onProfileClick = { userId ->
                            navController.navigate("profile/$userId")
                        }
                    )
                }
            },
            bottomBar = {
                val draftMessage by uiState.draftMessage.collectAsState()
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
                    CompletionDrawer {
                        if (uiState.files.isNotEmpty()) {
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
                                            imageVector = ImageVector.vectorResource(R.drawable.download),
                                            contentDescription = "Upload icon"
                                        )
                                    }

                                    Text(
                                        modifier = Modifier.fillMaxHeight()
                                            .wrapContentHeight(),
                                        text = name,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (currentEmojiCompletions.isNotEmpty()) {
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
                                            imageVector = Icons.Filled.Face,
                                            contentDescription = "Custom emoji"
                                        )
                                    }

                                    Text(
                                        modifier = Modifier.fillMaxHeight()
                                            .wrapContentHeight(),
                                        text = name,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (uiState.messageResponse != null) {
                            when (val content =
                                uiState.messageResponse!!.timelineEvent.content?.getOrNull()) {
                                is RoomMessageEventContent.TextBased -> {
                                    CompletionDrawerElement(
                                        0,
                                        onClick = { channelViewModel.unreplyMessage() }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(end = 5.dp)
                                                .size(24.dp)
                                                .align(Alignment.CenterVertically)
                                        ) {
                                            Icon(
                                                imageVector = when (uiState.messageResponse) {
                                                    is MessageResponse.ReplyTo -> ImageVector.vectorResource(
                                                        R.drawable.reply
                                                    )

                                                    is MessageResponse.Edit -> Icons.Filled.Edit
                                                    else -> Icons.Filled.Close
                                                },
                                                contentDescription = "What this is"
                                            )
                                        }

                                        Text(
                                            modifier = Modifier.fillMaxHeight()
                                                .wrapContentHeight(),
                                            text = content.body,
                                            fontSize = 11.sp,
                                            color = Color.LightGray,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                else -> {}
                            }
                        }
                    }

                    MessageBar(
                        uiState.channelInfo?.name?.explicitName ?: "?",
                        draftMessage,
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
    FilledTonalButton(
        modifier = Modifier
            .padding(
                start = 5.dp,
                end = 5.dp,
                bottom = 15.dp,
                top = if (index == 0) 15.dp else 0.dp
            )
            .height(38.dp)
            .fillMaxWidth() ,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(0.90f)
            ) {
                content()
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    modifier = Modifier.size(24.dp)
                        .padding(horizontal = 5.dp),
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Tap the button parent to close the item"
                )
            }
        }
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
