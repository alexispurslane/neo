package io.github.alexispurslane.bloc.ui.composables.misc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.BlockQuoteGutter
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.HeadingStyle
import com.halilibo.richtext.ui.InfoPanelStyle
import com.halilibo.richtext.ui.ListStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.TableStyle
import com.halilibo.richtext.ui.material3.Material3RichText
import com.halilibo.richtext.ui.string.RichTextStringStyle
import io.github.alexispurslane.bloc.data.models.User
import io.github.alexispurslane.bloc.viewmodels.ServerChannelUiState
import io.github.alexispurslane.bloc.viewmodels.ServerChannelViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room.Timeline
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.clientserverapi.model.users.GetProfile
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.ReactionEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent


@Composable
fun MessagesView(
    modifier: Modifier = Modifier,
    uiState: ServerChannelUiState,
    channelInfo: Room,
    channelViewModel: ServerChannelViewModel = hiltViewModel(),
    onProfileClick: (UserId) -> Unit = { },
    onMessageClick: (EventId) -> Unit = { }
) {
    val messages by uiState.messages!!.collectAsStateWithLifecycle()
    if (messages.isEmpty()) {
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

        LazyColumn(
            modifier = modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            reverseLayout = true,
            state = channelViewModel.messageListState
        ) {
            itemsIndexed(
                messages,
                key = { _, it -> it.eventId },
                contentType = { _, _ -> "Message" }
            ) { index, message ->
                Message(
                    modifier = Modifier
                        .padding(vertical = 5.dp),
                    currentUserId = uiState.currentUserId,
                    message,
                    channelViewModel.users[message.sender],
                    uiState.client,
                    messages.getOrNull(
                        index + 1
                    ),
                    onProfileClick,
                    onMessageClick
                )
            }
            if (uiState.atBeginning) {
                item(
                    contentType = "Beginning"
                ) {
                    BeginningMessage(
                        modifier = Modifier.height(200.dp),
                        channelInfo = channelInfo
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.LightGray,
                        )
                    }
                }
            }
        }

        val visible by remember { derivedStateOf { uiState.newMessages || channelViewModel.messageListState.firstVisibleItemIndex > 50 } }
        AnimatedVisibility(visible = visible) {
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

@Composable
fun BeginningMessage(
    modifier: Modifier = Modifier,
    channelInfo: Room
) {
    Box(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .padding(bottom = 5.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Text(
                "#${channelInfo.name}",
                fontWeight = FontWeight.Black,
                fontSize = 40.sp,
            )
            Text(
                "This is the beginning of your legendary conversation!",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun Message(
    modifier: Modifier = Modifier,
    currentUserId: String?,
    message: TimelineEvent,
    member: User?,
    client: MatrixClient?,
    prevMessage: TimelineEvent? = null,
    onProfileClick: (UserId) -> Unit = { },
    onMessageClick: (EventId) -> Unit = { }
) {
    when (val content = message.content?.getOrNull()) {
        is RoomMessageEventContent -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(
                        if (currentUserId != null && content.mentions?.users!!.contains(
                                UserId(currentUserId)
                            )
                        )
                            Color(0x55e3e312)
                        else
                            Color.Transparent
                    )
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    10.dp,
                    Alignment.Start
                ),
                verticalAlignment = Alignment.Top
            ) {
                if (member != null && (prevMessage == null || prevMessage.sender != message.sender)) {
                    UserAvatar(
                        size = 40.dp,
                        user = member,
                        client = client,
                        onClick = { userId ->
                            onProfileClick(userId)
                        }
                    )
                    Column(
                        modifier = Modifier.clickable {
                            onMessageClick(message.eventId)
                        }
                    ) {
                        Text(
                            member.displayName ?: member.userId.localpart,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                        )

                        MessageContent(content.body)

                        when (content) {
                            is RoomMessageEventContent.FileBased.Image -> {
                                if (content.url != null && client != null) {
                                    var collapseState by remember { mutableStateOf(true) }
                                    if (!collapseState) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 5.dp),
                                            verticalArrangement = Arrangement.spacedBy(
                                                5.dp,
                                                Alignment.CenterVertically
                                            ),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(MaterialTheme.shapes.large)
                                                    .height(200.dp)
                                            ) {
                                                MatrixImage(
                                                    mxcUri = content.url!!,
                                                    client = client
                                                )
                                            }
                                        }
                                    }
                                    TextButton(
                                        onClick = { collapseState = !collapseState }
                                    ) {
                                        Text(
                                            "${if (collapseState) "Expand" else "Collapse"} attachments",
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
        else -> {
            MessageContent(
                "Cannot access message content",
                color = Color.LightGray,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun MessageContent(
    content: String,
    color: Color = MaterialTheme.colorScheme.onBackground,
    fontSize: TextUnit = 18.sp,
    paragraphSpacing: TextUnit? = null,
    headingStyle: HeadingStyle? = null,
    listStyle: ListStyle? = null,
    blockQuoteGutter: BlockQuoteGutter? = null,
    codeBlockStyle: CodeBlockStyle? = null,
    tableStyle: TableStyle? = null,
    infoPanelStyle: InfoPanelStyle? = null,
    stringStyle: RichTextStringStyle? = RichTextStringStyle(
        linkStyle = SpanStyle(
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Black,
            textDecoration = TextDecoration.None,
        )
    )
) {
    ProvideTextStyle(
        TextStyle(
            color = color,
            fontSize = fontSize,
        )
    ) {
        Material3RichText(
            style = RichTextStyle(
                paragraphSpacing,
                headingStyle,
                listStyle,
                blockQuoteGutter,
                codeBlockStyle,
                tableStyle,
                infoPanelStyle,
                stringStyle,
            )
        ) {
            Markdown(content = content)
        }
    }
}