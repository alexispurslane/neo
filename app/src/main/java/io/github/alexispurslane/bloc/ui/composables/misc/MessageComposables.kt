package io.github.alexispurslane.bloc.ui.composables.misc

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider.getUriForFile
import androidx.hilt.navigation.compose.hiltViewModel
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
import io.github.alexispurslane.bloc.R
import io.github.alexispurslane.bloc.data.models.User
import io.github.alexispurslane.bloc.viewmodels.ServerChannelUiState
import io.github.alexispurslane.bloc.viewmodels.ChannelViewModel
import io.realm.kotlin.internal.interop.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.relatesTo
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.UnknownEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.toByteArray
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment
import java.io.File


@Composable
fun MessagesView(
    modifier: Modifier = Modifier,
    uiState: ServerChannelUiState,
    channelInfo: Room,
    channelViewModel: ChannelViewModel = hiltViewModel(),
    onProfileClick: (UserId) -> Unit = { },
) {
    val messages by uiState.messages!!.collectAsState()
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

        LaunchedEffect(uiState.messages) {
            if (!channelViewModel.messageListState.isScrollInProgress && channelViewModel.messageListState.firstVisibleItemIndex <= 3)
                channelViewModel.goToBottom()
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
                key = { _, it -> it.eventId.full },
                contentType = { _, _ -> "Message" }
            ) { index, message ->
                Message(
                    modifier = Modifier
                        .padding(vertical = 5.dp),
                    currentUserId = uiState.currentUserId,
                    message,
                    channelViewModel.users[message.sender],
                    messages.getOrNull(
                        index + 1
                    ),
                    onProfileClick,
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

fun launchActionWithAttachment(
    scope: CoroutineScope,
    context: Context,
    client: MatrixClient,
    uri: String,
    action: (Uri) -> Unit
) {
    scope.launch {
        client.media
            .getMedia(uri)
            .getOrNull()
            ?.let { bytes ->
                val newFile =
                    File.createTempFile(
                        "bloc",
                        "attachment",
                        context.getExternalFilesDir(
                            "attachments"
                        )
                    )
                newFile.writeBytes(bytes.toByteArray())
                val fileUri = getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    newFile
                )
                action(fileUri)
            }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Message(
    modifier: Modifier = Modifier,
    currentUserId: String?,
    message: TimelineEvent,
    member: User?,
    prevMessage: TimelineEvent? = null,
    onProfileClick: (UserId) -> Unit = { },
    channelViewModel: ChannelViewModel = hiltViewModel()
) {
    val channelUiState by channelViewModel.uiState.collectAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(
            10.dp,
            Alignment.Start
        ),
        verticalAlignment = Alignment.Top
    ) {
        if (member != null && message.relatesTo !is RelatesTo.Replace) {
            val avatarSize = (channelUiState.fontSize.value * 2 + 8)
            if (prevMessage == null || prevMessage.sender != message.sender) {
                UserAvatar(
                    size = avatarSize.dp,
                    user = member,
                    client = channelUiState.client,
                    onClick = { userId ->
                        onProfileClick(userId)
                    }
                )
            } else {
                Spacer(modifier = Modifier.width(avatarSize.dp))
            }
            Column {
                if (prevMessage == null || prevMessage.sender != message.sender) {
                    Text(
                        member.displayName ?: member.userId.localpart,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Start,
                    )
                }
                when (val content = message.content?.getOrNull()) {
                    is UnknownEventContent  -> {
                        if (content.eventType == "m.sticker") {
                            if (channelUiState.client != null) {
                                MatrixImage(
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.large)
                                        .size(250.dp),
                                    client = channelUiState.client!!,
                                    mxcUri = content.raw["url"]!!.jsonPrimitive.content
                                )
                            } else {
                                MessageContent(content = "Sticker: ${content.raw["body"]!!}")
                            }
                        } else {
                            MessageContent(
                                "Unknown timeline event: ${message.content.toString()}",
                                color = Color.LightGray,
                                fontSize = 16.sp
                            )
                        }
                    }
                    is RoomMessageEventContent -> {
                        val clipboard = LocalClipboardManager.current

                        var collapseState by remember { mutableStateOf(!channelUiState.expandImages) }

                        val context = LocalContext.current
                        val scope = rememberCoroutineScope()
                        when (content) {
                            is RoomMessageEventContent.TextBased -> {
                                MessageContent(
                                    content.body,
                                    modifier = Modifier
                                        .combinedClickable(
                                            onLongClick = { },
                                            onClick = { clipboard.setText(AnnotatedString(content.body)) }
                                        )
                                        .border(
                                            width = 5.dp,
                                            color = if (currentUserId != null && content.mentions?.users?.contains(
                                                    UserId(currentUserId)
                                                ) == true
                                            )
                                                Color.Yellow
                                            else
                                                Color.Transparent
                                        ),
                                    fontSize = channelUiState.fontSize,
                                    textAlign = if (channelUiState.justifyText) TextAlign.Justify else TextAlign.Start
                                )
                            }
                            is RoomMessageEventContent.FileBased.File -> {
                                if (content.url != null && channelUiState.client != null) {
                                    TextButton(onClick = {
                                        launchActionWithAttachment(scope, context, channelUiState.client!!, content.url!!) {
                                            val intent =
                                                Intent(Intent.ACTION_CREATE_DOCUMENT)
                                            intent.setDataAndType(
                                                it,
                                                content.info?.mimeType
                                            )
                                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            context.startActivity(intent)
                                        }
                                    }) {
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 5.dp)
                                                .size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.download),
                                                contentDescription = "Download file"
                                            )
                                        }

                                        Text(text = content.body)
                                    }
                                }
                            }
                            is RoomMessageEventContent.FileBased.Image -> {
                                if (content.url != null && channelUiState.client != null) {
                                    if (!collapseState) {
                                        MatrixImage(
                                            modifier = Modifier
                                                .padding(vertical = 5.dp)
                                                .clip(MaterialTheme.shapes.large)
                                                .height(300.dp)
                                                .clickable {
                                                    launchActionWithAttachment(
                                                        scope,
                                                        context,
                                                        channelUiState.client!!,
                                                        content.url!!
                                                    ) {
                                                        val intent =
                                                            Intent(Intent.ACTION_VIEW)
                                                        intent.setDataAndType(
                                                            it,
                                                            content.info?.mimeType
                                                        )
                                                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        context.startActivity(intent)
                                                    }
                                                },
                                            mxcUri = content.url!!,
                                            client = channelUiState.client!!,
                                            shouldFill = false
                                        )
                                    }
                                    TextButton(
                                        modifier = Modifier.padding(start = 0.dp),
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

                    else -> {
                        MessageContent(
                            "Unknown timeline event: ${message.content.toString()}",
                            color = Color.LightGray,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MessageContent(
    content: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    fontSize: TextUnit = 18.sp,
    textAlign: TextAlign = TextAlign.Justify,
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
            textAlign = textAlign
        )
    ) {
        Material3RichText(
            modifier = modifier,
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