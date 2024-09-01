package io.github.alexispurslane.bloc.ui.composables.misc

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.text.Editable
import android.text.Html.ImageGetter
import android.text.Html.TagHandler
import android.text.Layout
import android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.Spanned.SPAN_MARK_MARK
import android.text.style.AlignmentSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.aghajari.compose.text.AnnotatedText
import com.aghajari.compose.text.ContentAnnotatedString
import com.aghajari.compose.text.asAnnotatedString
import io.github.alexispurslane.bloc.R
import io.github.alexispurslane.bloc.data.asUser
import io.github.alexispurslane.bloc.data.fullUserId
import io.github.alexispurslane.bloc.data.models.User
import io.github.alexispurslane.bloc.ui.theme.EngineeringOrange
import io.github.alexispurslane.bloc.viewmodels.ServerChannelUiState
import io.github.alexispurslane.bloc.viewmodels.ChannelViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import net.folivo.trixnity.core.model.events.RoomEventContent
import net.folivo.trixnity.core.model.events.UnknownEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.utils.toByteArray
import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.XMLReader
import java.io.File
import kotlin.math.roundToLong

@Composable
private fun LazyListState.containItem(index:Int): Boolean {

    return remember(this) {
        derivedStateOf {
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                visibleItemsInfo.toMutableList().map { it.index }.contains(index)
            }
        }
    }.value
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesView(
    modifier: Modifier = Modifier,
    uiState: ServerChannelUiState,
    channelInfo: Room,
    messages: List<TimelineEvent>,
    onProfileClick: (UserId) -> Unit = { },
    channelViewModel: ChannelViewModel = hiltViewModel(),
) {
    if (messages.isEmpty()) {
        BeginningMessage(
            modifier = modifier.fillMaxSize(),
            channelInfo = channelInfo
        )
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            val atTop by remember { derivedStateOf { !channelViewModel.messageListState.canScrollForward } }
            LaunchedEffect(atTop) {
                if (atTop) {
                    channelViewModel.fetchEarlierMessages()
                }
            }

            LaunchedEffect(messages) {
                if (!channelViewModel.messageListState.isScrollInProgress && channelViewModel.messageListState.firstVisibleItemIndex <= 5)
                    channelViewModel.goToBottom()
            }

            var bottomSheetCurrentMessage by remember { mutableStateOf<EventId?>(null) }
            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = false
            )

            val outbox = uiState.outbox?.collectAsState()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.Start,
                reverseLayout = true,
                state = channelViewModel.messageListState
            ) {
                if (outbox != null) {
                    Log.d("Message View", "outbox non null")
                    val outboxState by outbox
                    itemsIndexed(outboxState) { index, item ->
                        val space = (uiState.fontSize.value * 2 + 8).dp
                        // NOTE: only checking the outbox against the last *6* messages in the timeline is purely arbitrary, the outbox just never seems to contain a message from older than that, so why waste CPU cycles checking further?
                        if (item != null && !messages.take(6).fastAny { it.eventId == item.eventId }) {
                            Log.d("Message View", "displayed outbox item, $item")
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 25.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier.padding(start = space)
                                ) {
                                    MessageContent(
                                        content = Result.success(item.content as RoomEventContent),
                                        eventId = item.eventId,
                                        member = uiState.currentUserInfo?.asUser(),
                                        lazyListState = channelViewModel.messageListState,
                                        index = index,
                                        channelUiState = uiState,
                                        textColor = Color.DarkGray
                                    )
                                }
                                Icon(
                                    modifier = Modifier.size(24.dp),
                                    imageVector = ImageVector.vectorResource(R.drawable.clock),
                                    contentDescription = "Message sending...",
                                    tint = Color.DarkGray
                                )
                            }
                        }
                    }
                }

                itemsIndexed(
                    messages,
                    key = { _, it -> it.eventId.full },
                    contentType = { _, _ -> "Message" }
                ) { index, message ->
                    Message(
                        modifier = Modifier
                            .padding(vertical = 5.dp),
                        message = message,
                        prevMessage = messages.getOrNull(
                            index + 1
                        ),
                        onProfileClick = onProfileClick,
                        lazyListState = channelViewModel.messageListState,
                        index = index,
                        onMessageTapHold = {
                            bottomSheetCurrentMessage = it
                        }
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

            if (bottomSheetCurrentMessage != null) {
                ModalBottomSheet(
                    modifier = Modifier.fillMaxHeight(),
                    sheetState = sheetState,
                    onDismissRequest = { bottomSheetCurrentMessage = null }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                            .padding(horizontal = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        FilledTonalButton(
                            onClick = { }
                        ) {
                            Text("Delete")
                        }

                        FilledTonalButton(
                            onClick = { }
                        ) {
                            Text("Edit")
                        }

                        FilledTonalButton(
                            onClick = { }
                        ) {
                            Text("Reply")
                        }
                    }
                }
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
                "#${channelInfo.name?.explicitName}",
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
    message: TimelineEvent,
    prevMessage: TimelineEvent? = null,
    lazyListState: LazyListState,
    index: Int,
    onProfileClick: (UserId) -> Unit = { },
    onMessageTapHold: (EventId) -> Unit = { },
    channelViewModel: ChannelViewModel = hiltViewModel(),
) {
    val channelUiState by channelViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp)
            .combinedClickable(
                onClick = {
                    focusManager.clearFocus(true)
                },
                onLongClick = {
                    onMessageTapHold(message.eventId)
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(
            10.dp,
            Alignment.Start
        ),
        verticalAlignment = Alignment.Top
    ) {
        val scope = rememberCoroutineScope()
        var memberState by remember { mutableStateOf<User?>(null) }
        val visible = lazyListState.containItem(index)
        LaunchedEffect(visible) {
            scope.launch {
                val member = channelViewModel.fetchuserInformation(message.sender)
                memberState = member.await()
            }
        }
        val avatarSize = (channelUiState.fontSize.value * 2 + 8)
        if (memberState != null && (prevMessage == null || prevMessage.sender != message.sender)) {
            UserAvatar(
                size = avatarSize.dp,
                user = memberState!!,
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
                    memberState?.displayName ?: memberState?.userId?.localpart ?: message.sender.localpart,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Start,
                )
            }
            MessageContent(
                content = message.content,
                eventId = message.eventId,
                member = memberState,
                channelUiState = channelUiState,
                lazyListState = lazyListState,
                index = index
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageContent(
    content: Result<RoomEventContent>?,
    eventId: EventId?,
    member: User?,
    lazyListState: LazyListState?,
    index: Int,
    channelUiState: ServerChannelUiState,
    channelViewModel: ChannelViewModel = hiltViewModel(),
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    when (val content = content?.getOrNull()) {
        is UnknownEventContent  -> {
            if (content.eventType == "m.sticker") {
                if (channelUiState.client != null) {
                    MatrixImage(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.large)
                            .size(250.dp),
                        client = channelUiState.client,
                        mxcUri = content.raw["url"]!!.jsonPrimitive.content
                    )
                } else {
                    MessageBody(content = "Sticker: ${content.raw["body"]!!}")
                }
            } else {
                MessageBody(
                    "Unknown timeline event: $content",
                    textColor = Color.LightGray,
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
                    val prefix = when (content) {
                        is RoomMessageEventContent.TextBased.Emote -> "${member?.displayName} "
                        is RoomMessageEventContent.TextBased.Notice -> "${channelUiState.channelInfo?.name?.explicitName} notice: "
                        else -> ""
                    }
                    MessageBody(
                        content = prefix + content.body,
                        formattedContent = content.formattedBody?.let { prefix + it },
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = { },
                                onClick = { clipboard.setText(AnnotatedString(content.body)) }
                            )
                            .border(
                                width = 5.dp,
                                // TODO: a lot of clients don't use the m.mentions protocol, so we need a robust way to detect mentions anyway
                                color = if (channelUiState.currentUserInfo?.fullUserId() != null && (content.mentions?.users?.contains(
                                        UserId(channelUiState.currentUserInfo.fullUserId())
                                    ) == true)
                                )
                                    Color.Yellow
                                else
                                    Color.Transparent
                            ),
                        textColor = when (content) {
                            is RoomMessageEventContent.TextBased.Emote -> Color.Green
                            is RoomMessageEventContent.TextBased.Notice -> Color.Yellow
                            else -> textColor
                        },
                        fontStyle = when (content) {
                            is RoomMessageEventContent.TextBased.Emote -> FontStyle.Italic
                            else -> null
                        },
                        fontSize = channelUiState.fontSize,
                        textAlign = if (channelUiState.justifyText) TextAlign.Justify else TextAlign.Start,
                        client = channelUiState.client,
                        lazyListState = lazyListState,
                        index = index
                    )
                    if (channelUiState.client != null) {
                        val reactions by remember { derivedStateOf { channelViewModel.reactions[eventId] } }
                        reactions?.let {
                            val reactionsState by it.collectAsState()
                            Row(
                                modifier = Modifier.padding(top = 5.dp)
                            ) {
                                reactionsState.forEach { (emoji, eventUsers) ->
                                    val userReactEvent = eventUsers.entries.find {
                                        it.value == UserId(channelUiState.currentUserInfo!!.fullUserId())
                                    }

                                    FilledTonalButton(
                                        contentPadding = PaddingValues(3.dp),
                                        colors = if (userReactEvent != null)
                                            ButtonDefaults.buttonColors()
                                        else
                                            ButtonDefaults.filledTonalButtonColors(),
                                        onClick = {
                                            channelViewModel.react(eventId!!, emoji, userReactEvent?.key)
                                        },
                                    ) {
                                        if (emoji.startsWith("mxc")) {
                                            val resources = LocalContext.current.resources
                                            MatrixImage(
                                                modifier = Modifier
                                                    .size(
                                                        (resources.displayMetrics.density * 13.sp.value * 0.6).dp

                                                    )
                                                    .align(Alignment.CenterVertically),
                                                client = channelUiState.client,
                                                mxcUri = emoji
                                            )
                                        } else {
                                            Text(emoji, fontSize = 13.sp)
                                        }

                                        Text(
                                            modifier = Modifier.padding(start = 5.dp),
                                            text = eventUsers.size.toString(),
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is RoomMessageEventContent.FileBased.File -> {
                    if (content.url != null && channelUiState.client != null) {
                        TextButton(onClick = {
                            launchActionWithAttachment(scope, context, channelUiState.client, content.url!!) {
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
                                            channelUiState.client,
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
                                client = channelUiState.client,
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
            MessageBody(
                "Unknown timeline event: ${content.toString()}",
                textColor = Color.LightGray,
                fontSize = 16.sp
            )
        }
    }
}

// This code is copied from Html.android.kt in the SDK because THEY WON'T MAKE ANYTHING PUBLIC so if I want to create a version of one method, I have to copy more and more of the API features it depends on out, because they're private.
// Down with private class properties!

fun AlignmentSpan.toParagraphStyle(): ParagraphStyle {
    val alignment = when (this.alignment) {
        Layout.Alignment.ALIGN_NORMAL -> TextAlign.Start
        Layout.Alignment.ALIGN_CENTER -> TextAlign.Center
        Layout.Alignment.ALIGN_OPPOSITE -> TextAlign.End
        else -> TextAlign.Unspecified
    }
    return ParagraphStyle(textAlign = alignment)
}

fun StyleSpan.toSpanStyle(): SpanStyle? {
    /** StyleSpan doc: styles are cumulative -- if both bold and italic are set in
     * separate spans, or if the base style is bold and a span calls for italic,
     * you get bold italic.  You can't turn off a style from the base style.
     */
    return when (style) {
        Typeface.BOLD -> {
            SpanStyle(fontWeight = FontWeight.Bold)
        }
        Typeface.ITALIC -> {
            SpanStyle(fontStyle = FontStyle.Italic)
        }
        Typeface.BOLD_ITALIC -> {
            SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        }
        else -> null
    }
}

fun TypefaceSpan.toSpanStyle(): SpanStyle {
    val fontFamily = when (family) {
        FontFamily.Cursive.name -> FontFamily.Cursive
        FontFamily.Monospace.name -> FontFamily.Monospace
        FontFamily.SansSerif.name -> FontFamily.SansSerif
        FontFamily.Serif.name -> FontFamily.Serif
        else -> { FontFamily.SansSerif }
    }
    return SpanStyle(fontFamily = fontFamily)
}

val TagHandler = object : TagHandler {
    override fun handleTag(
        opening: Boolean,
        tag: String?,
        output: Editable?,
        xmlReader: XMLReader?
    ) {
        if (xmlReader == null || output == null) return

        if (opening && tag == "ContentHandlerReplacementTag") {
            val currentContentHandler = xmlReader.contentHandler
            xmlReader.contentHandler = AnnotationContentHandler(currentContentHandler, output)
        }
    }
}

class AnnotationContentHandler(
    private val contentHandler: ContentHandler,
    private val output: Editable
) : ContentHandler by contentHandler {
    override fun startElement(uri: String?, localName: String?, qName: String?, atts: Attributes?) {
        if (localName == "annotation") {
            atts?.let { handleAnnotationStart(it) }
        } else {
            contentHandler.startElement(uri, localName, qName, atts)
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        if (localName == "annotation") {
            handleAnnotationEnd()
        } else {
            contentHandler.endElement(uri, localName, qName)
        }
    }

    private fun handleAnnotationStart(attributes: Attributes) {
        // Each annotation can have several key/value attributes. So for
        // <annotation key1=value1 key2=value2>...<annotation>
        // example we will add two [AnnotationSpan]s which we'll later read
        for (i in 0 until attributes.length) {
            val key = attributes.getLocalName(i).orEmpty()
            val value = attributes.getValue(i).orEmpty()
            if (key.isNotEmpty() && value.isNotEmpty()) {
                val start = output.length
                // add temporary AnnotationSpan to the output to read it when handling
                // the closing tag
                output.setSpan(AnnotationSpan(key, value), start, start, SPAN_MARK_MARK)
            }
        }
    }

    private fun handleAnnotationEnd() {
        // iterate through all of the spans that we added when handling the opening tag. Calculate
        // the true position of the span and make a replacement
        output.getSpans(0, output.length, AnnotationSpan::class.java)
            .filter { output.getSpanFlags(it) == SPAN_MARK_MARK }
            .fastForEach { annotation ->
                val start = output.getSpanStart(annotation)
                val end = output.length

                output.removeSpan(annotation)
                // only add the annotation if there's a text in between the opening and closing tags
                if (start != end) {
                    output.setSpan(annotation, start, end, SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
    }
}

class AnnotationSpan(val key: String, val value: String)

@Composable
fun MessageBody(
    content: String,
    formattedContent: String? = null,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    fontSize: TextUnit = 18.sp,
    textAlign: TextAlign = TextAlign.Justify,
    fontStyle: FontStyle? = null,
    paragraphSpacing: TextUnit? = null,
    client: MatrixClient? = null,
    lazyListState: LazyListState? = null,
    index: Int = -1
) {
    var annotatedString by remember { mutableStateOf(ContentAnnotatedString(AnnotatedString(content), inlineContents = listOf(), paragraphContents = listOf())) }
    val resources = LocalContext.current.resources
    val visible = lazyListState?.containItem(index)
    val linkColor = MaterialTheme.colorScheme.secondary
    val size = (resources.displayMetrics.density * fontSize.value).roundToLong()
    LaunchedEffect(formattedContent, visible) {
        if (visible == true && formattedContent != null) {
            launch(Dispatchers.IO) {
                // NOTE: keep this up to date with the real `ContentHandlerReplacementTag` variable
                val cleanedHtmlString =
                    formattedContent
                        .replace(Regex("</?body>"), "") // remove default wrapping body tag
                        .replace(Regex("(^<p>|</p>$)"), "") // remove default wrapping paragraph
                        .let {
                            if (it.lines().size > 1) it
                                .replace(Regex("(^|\n)(.*)(\n|$)"), "<p>$2</p>") // re-add paragraphs only where desirable
                            else it
                        }
                val stringToParse = "<ContentHandlerReplacementTag />$cleanedHtmlString"
                val spanned = HtmlCompat.fromHtml(
                    stringToParse,
                    HtmlCompat.FROM_HTML_MODE_LEGACY,
                    if (client != null)
                        ImageGetter { source ->
                            if (source!!.startsWith("mxc", ignoreCase = true)) {
                                runBlocking {
                                    client.media.getThumbnail(source, height = size, width = size).getOrNull()?.toByteArray()?.let { bytes ->
                                        BitmapDrawable(resources, BitmapFactory.decodeByteArray(
                                            bytes,
                                            0,
                                            bytes.size
                                        ))
                                    }
                                }
                            } else {
                                null
                            }
                        }
                    else null,
                    TagHandler // TODO: use this to style blockquotes, custom emoji, and replies, and so on
                )
                annotatedString = spanned.asAnnotatedString(
                    linkColor = linkColor
                )
            }
        }
    }

    SelectionContainer {
        AnnotatedText(
            text = annotatedString,
            color = textColor,
            fontSize = fontSize,
            textAlign = textAlign,
            fontStyle = fontStyle
        )
    }
}

