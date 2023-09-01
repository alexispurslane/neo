package io.github.alexispurslane.bloc.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.alexispurslane.bloc.LoadingScreen
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.ui.composables.misc.BeginningMessage
import io.github.alexispurslane.bloc.ui.composables.misc.Message
import io.github.alexispurslane.bloc.ui.composables.navigation.ChannelTopBar
import io.github.alexispurslane.bloc.ui.composables.navigation.MessageBar
import io.github.alexispurslane.bloc.viewmodels.ServerChannelUiState
import io.github.alexispurslane.bloc.viewmodels.ServerChannelViewModel
import kotlinx.coroutines.launch

@Composable
fun ChannelViewScreen(
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
                            channelInfo
                        )
                    }
                },
                bottomBar = {
                    MessageBar(channelInfo.name)
                }
            )
        }
    }
}


@Composable
fun MessagesView(
    modifier: Modifier = Modifier,
    uiState: ServerChannelUiState,
    channelInfo: RevoltChannel.TextChannel,
    channelViewModel: ServerChannelViewModel = hiltViewModel()
) {
    if (uiState.messages.isEmpty()) {
        BeginningMessage(
            modifier = modifier.fillMaxSize(),
            channelInfo = channelInfo
        )
    } else {
        val configuration = LocalConfiguration.current

        val atTop by remember { derivedStateOf { !channelViewModel.messageListState.canScrollForward } }
        LaunchedEffect(atTop) {
            if (atTop) {
                channelViewModel.fetchEarlierMessages()
            }
        }
        Column(
            modifier = Modifier.height(Dp(configuration.screenHeightDp.toFloat()))
        ) {
            LazyColumn(
                modifier = modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(
                    5.dp,
                    Alignment.Bottom
                ),
                horizontalAlignment = Alignment.Start,
                reverseLayout = true,
                state = channelViewModel.messageListState
            ) {
                itemsIndexed(
                    uiState.messages,
                    key = { _, it -> it.messageId }) { index, message ->
                    val (user, member) = uiState.users[message.authorId]!!
                    Message(
                        modifier = Modifier.background(
                            if (message.mentionedIds?.contains(
                                    uiState.currentUserId
                                ) == true
                            )
                                Color(0x55FFFF00)
                            else
                                Color.Transparent
                        ),
                        message,
                        user,
                        member,
                        role = uiState.serverInfo?.roles?.filterKeys {
                            member.roles?.contains(
                                it
                            ) ?: false
                        }?.values?.minByOrNull { it.rank },
                        prevMessage = uiState.messages.getOrNull(
                            index + 1
                        )
                    )
                }
                if (uiState.atBeginning) {
                    item {
                        BeginningMessage(
                            modifier = Modifier.height(200.dp),
                            channelInfo = channelInfo
                        )
                    }
                }
            }

            val scrolledBack by remember { derivedStateOf { channelViewModel.messageListState.firstVisibleItemIndex > 24 } }
            if (uiState.newMessages || scrolledBack) {
                val coroutineScope = rememberCoroutineScope()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(30.dp)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            coroutineScope.launch {
                                channelViewModel.goToBottom()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "New messages available",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
