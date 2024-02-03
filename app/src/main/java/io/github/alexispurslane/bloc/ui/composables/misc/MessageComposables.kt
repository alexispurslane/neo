package io.github.alexispurslane.bloc.ui.composables.misc

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
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
import io.github.alexispurslane.bloc.data.local.RevoltAutumnModule
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.data.network.models.RevoltFileMetadata
import io.github.alexispurslane.bloc.data.network.models.RevoltMessage
import io.github.alexispurslane.bloc.data.network.models.RevoltServerMember
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import io.github.alexispurslane.bloc.data.network.models.Role
import io.github.alexispurslane.bloc.ui.theme.AppFont
import io.github.alexispurslane.bloc.ui.theme.EngineeringOrange
import io.github.alexispurslane.bloc.viewmodels.ServerChannelUiState
import io.github.alexispurslane.bloc.viewmodels.ServerChannelViewModel
import kotlinx.coroutines.launch

@Composable
fun MessagesView(
    modifier: Modifier = Modifier,
    uiState: ServerChannelUiState,
    channelInfo: RevoltChannel.TextChannel,
    channelViewModel: ServerChannelViewModel = hiltViewModel(),
    onProfileClick: (String) -> Unit = { },
    onMessageClick: (String, String?, Boolean) -> Unit = { a, b, c -> }
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

        LazyColumn(
            modifier = modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            reverseLayout = true,
            state = channelViewModel.messageListState
        ) {
            itemsIndexed(
                uiState.messages,
                key = { _, it -> "${it.messageId}${it.edited}" },
                contentType = { _, _ -> "Message" }
            ) { index, message ->
                val entry = uiState.users[message.authorId]
                Message(
                    modifier = Modifier
                        .padding(vertical = 5.dp),
                    message,
                    entry?.first,
                    entry?.second,
                    role = uiState.serverInfo?.roles?.filterKeys {
                        entry?.second?.roles?.contains(
                            it
                        ) ?: false
                    }?.values?.minByOrNull { it.rank },
                    prevMessage = uiState.messages.getOrNull(
                        index + 1
                    ),
                    uiState.currentUserId,
                    onProfileClick,
                    onMessageClick,
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
                ProvideTextStyle(value = MaterialTheme.typography.labelMedium) {
                    Text("New messages available")
                }
            }
        }
    }
}

@Composable
fun BeginningMessage(
    modifier: Modifier = Modifier,
    channelInfo: RevoltChannel.TextChannel
) {
    Box(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .padding(bottom = 5.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            ProvideTextStyle(value = MaterialTheme.typography.displayMedium) {
                Text("#${channelInfo.name}")
            }

            Text(
                "This is the beginning of your legendary conversation!",
                fontWeight = FontWeight.Black,
                fontFamily = AppFont.Metropolis,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun dpToSp(dp: Dp) = with(LocalDensity.current) { dp.toSp() }

@Composable
fun Message(
    modifier: Modifier = Modifier,
    message: RevoltMessage,
    user: RevoltUser?,
    member: RevoltServerMember?,
    role: Role?,
    prevMessage: RevoltMessage? = null,
    currentUserId: String?,
    onProfileClick: (String) -> Unit = { },
    onMessageClick: (String, String?, Boolean) -> Unit = { a, b, c -> },
) {
    Row(
        modifier = modifier
            .background(
                if (message.mentionedIds?.contains(
                        currentUserId
                    ) == true
                )
                    Color(0x55e3e312)
                else
                    Color.Transparent
            )
            .fillMaxWidth()
            .padding(horizontal = 15.dp)
            .clickable { onMessageClick(message.messageId, null, false) },
        horizontalArrangement = Arrangement.spacedBy(
            10.dp,
            Alignment.Start
        ),
        verticalAlignment = Alignment.Top
    ) {
        if (user != null && member != null) {
            if (prevMessage == null || prevMessage.authorId != message.authorId) {
                UserAvatar(
                    size = 40.dp,
                    userProfile = user,
                    member = member,
                    masquerade = message.masquerade,
                    onClick = { userId ->
                        onProfileClick(userId)
                    }
                )
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }
            Column(
                modifier = Modifier.clickable {
                    onMessageClick(message.messageId, null, false)
                }
            ) {
                if (prevMessage == null || prevMessage.authorId != message.authorId) {
                    ProvideTextStyle(value = MaterialTheme.typography.titleMedium) {
                        Text(
                            message.masquerade?.name ?: member.nickname
                            ?: user.displayName ?: user.userName,
                            textAlign = TextAlign.Start,
                            color = if (role != null) {
                                try {
                                    Color(
                                        android.graphics.Color.parseColor(
                                            message.masquerade?.color
                                                ?: role.color
                                        )
                                    )
                                } catch (e: Exception) {
                                    Color.White
                                }
                            } else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                if (message.content!!.isNotBlank()) {
                    CustomizableMarkdownText(
                        message.content,
                    )
                }
                if (message.reactions?.isNotEmpty() == true) {
                    Row {
                        message.reactions.forEach {
                            val isUsersReaction =
                                it.value.contains(currentUserId)
                            Row(
                                modifier = Modifier
                                    .padding(end = 3.dp)
                                    .let { m ->
                                        if (isUsersReaction) {
                                            m.border(
                                                1.dp,
                                                EngineeringOrange,
                                                RoundedCornerShape(3.dp)
                                            )
                                        } else {
                                            m.clip(RoundedCornerShape(3.dp))
                                        }
                                    }
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(3.dp)
                                    .clickable {
                                        onMessageClick(
                                            message.messageId,
                                            it.key,
                                            isUsersReaction
                                        )
                                    }
                            ) {
                                val split = it.key.split(":")
                                if (split.size > 1) {
                                    AsyncImage(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .aspectRatio(1f)
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        model = split[0],
                                        contentDescription = "Reaction emoji: ${split[1]}",
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(3.dp))
                                            .padding(3.dp)
                                    ) {
                                        Text(
                                            fontSize = dpToSp(dp = 12.dp),
                                            text = split[0]
                                        )
                                    }
                                }
                                Text(
                                    fontSize = dpToSp(dp = 15.dp),
                                    text = it.value.size.toString()
                                )
                            }
                        }
                    }
                }
                if (message.attachments?.isNotEmpty() == true) {
                    var collapseState by remember { mutableStateOf(message.attachments.size > 2) }
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
                            val uriHandler = LocalUriHandler.current
                            message.attachments.forEachIndexed { index, autumnFile ->
                                val url =
                                    RevoltAutumnModule.getResourceUrl(
                                        LocalContext.current,
                                        autumnFile
                                    )
                                when (autumnFile.metadata) {
                                    is RevoltFileMetadata.Image -> {
                                        AsyncImage(
                                            modifier = Modifier
                                                .height(256.dp)
                                                .clip(MaterialTheme.shapes.large)
                                                .clickable {
                                                    if (url != null)
                                                        uriHandler.openUri(url)
                                                },
                                            model = url,
                                            contentDescription = "image attachment $index",
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    else -> {}
                                }
                            }
                        }
                    }
                    ProvideTextStyle(value = MaterialTheme.typography.labelLarge) {
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
            }
        } else if (message.authorId == "00000000000000000000000000" && message.systemEventMessage != null) {
            CustomizableMarkdownText(
                message.systemEventMessage.message,
                fontWeight = FontWeight.Black,
                color = Color.Gray,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun CustomizableMarkdownText(
    content: String,
    color: Color = MaterialTheme.colorScheme.onBackground,
    fontSize: TextUnit = 18.sp,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontSynthesis: FontSynthesis? = null,
    fontFamily: FontFamily? = null,
    fontFeatureSettings: String? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    baselineShift: BaselineShift? = null,
    textGeometricTransform: TextGeometricTransform? = null,
    localeList: LocaleList? = null,
    background: Color = Color.Transparent,
    textDecoration: TextDecoration? = null,
    shadow: Shadow? = null,
    textAlign: TextAlign? = null,
    textDirection: TextDirection? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textIndent: TextIndent? = null,
    platformStyle: PlatformTextStyle? = null,
    lineHeightStyle: LineHeightStyle? = null,
    lineBreak: LineBreak? = null,
    hyphens: Hyphens? = null,
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
            fontWeight = FontWeight.ExtraBold,
            textDecoration = TextDecoration.None,
        )
    )
) {
    SelectionContainer {
        ProvideTextStyle(
            MaterialTheme.typography.bodyLarge.merge(
                TextStyle(
                    color,
                    fontSize,
                    fontWeight,
                    fontStyle,
                    fontSynthesis,
                    fontFamily,
                    fontFeatureSettings,
                    letterSpacing,
                    baselineShift,
                    textGeometricTransform,
                    localeList,
                    background,
                    textDecoration,
                    shadow,
                    textAlign,
                    textDirection,
                    lineHeight,
                    textIndent,
                    platformStyle,
                    lineHeightStyle,
                    lineBreak,
                    hyphens
                )
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
}