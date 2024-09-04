package io.github.alexispurslane.neo.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.alexispurslane.neo.data.AccountsRepository
import io.github.alexispurslane.neo.data.MessagesRepository
import io.github.alexispurslane.neo.data.RoomsRepository
import io.github.alexispurslane.neo.data.UserSession
import io.github.alexispurslane.neo.data.flattenFlow
import io.github.alexispurslane.neo.data.models.User
import io.ktor.http.ContentType
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.audio
import net.folivo.trixnity.client.room.message.emote
import net.folivo.trixnity.client.room.message.file
import net.folivo.trixnity.client.room.message.image
import net.folivo.trixnity.client.room.message.notice
import net.folivo.trixnity.client.room.message.react
import net.folivo.trixnity.client.room.message.replace
import net.folivo.trixnity.client.room.message.reply
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomOutboxMessage
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import javax.inject.Inject

abstract class MessageResponse(open val timelineEvent: TimelineEvent) {
    data class ReplyTo(override val timelineEvent: TimelineEvent) : MessageResponse(timelineEvent)
    data class Edit(override val timelineEvent: TimelineEvent) : MessageResponse(timelineEvent)
}

data class ServerChannelUiState(
    val channelId: String? = null,
    val channelInfo: Room? = null,
    val currentUserInfo: UserSession? = null,
    val client: MatrixClient? = null,
    val error: String? = null,
    val atBeginning: Boolean = false,
    val newMessages: Boolean = false,
    val draftMessage: MutableStateFlow<String> = MutableStateFlow(""),
    val isSendError: Boolean = false,
    val sendErrorTitle: String = "",
    val sendErrorText: String = "",
    val messages: StateFlow<List<TimelineEvent>>? = null,
    val outbox: StateFlow<List<RoomOutboxMessage<*>?>>? = null,
    val fontSize: TextUnit = 16.sp,
    val justifyText: Boolean = true,
    val expandImages: Boolean = true,
    val files: Map<String, Uri> = mapOf(),
    val messageResponse: MessageResponse? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val roomsRepository: RoomsRepository,
    private val messagesRepository: MessagesRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerChannelUiState())
    val uiState: StateFlow<ServerChannelUiState> = _uiState.asStateFlow()

    val parser = MarkdownParser(CommonMarkFlavourDescriptor())

    val reactions
        get() = messagesRepository.messageReactions

    val messageListState = LazyListState()

    init {
        viewModelScope.launch {
            accountsRepository.userSessionFlow.collect {
                if (it?.userId != null) {
                    _uiState.update { prevState ->
                        Log.d("Channel View", it.preferences["fontSize"].toString())
                        prevState.copy(
                            currentUserInfo = it,
                            fontSize = it.preferences["fontSize"]?.toFloatOrNull()?.sp ?: 16.sp,
                            justifyText = it.preferences["justifyText"]?.toBooleanStrictOrNull() ?: true,
                            expandImages = it.preferences["expandImages"]?.toBooleanStrictOrNull() ?: true,
                        )
                    }
                }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            savedStateHandle.getStateFlow("channelId", null)
                .collectLatest { channelId: String? ->
                    if (channelId != null) {
                        Log.d("Channel View", "channel visited: $channelId")
                        launch {
                            _uiState.update {
                                initializeChannelData(channelId, it)
                            }
                        }

                        launch {
                            accountsRepository.matrixClient?.room!!.getOutbox().collectLatest {
                                val messagesForRoom = it.values.filter { it.first()?.roomId == RoomId(channelId) }
                                _uiState.update {
                                    it.copy(
                                        outbox = messagesForRoom.flattenFlow().stateIn(viewModelScope)
                                    )
                                }
                            }
                        }
                    }
                }
        }

        viewModelScope.launch {
            accountsRepository.matrixClientFlow.collectLatest { mc ->
                _uiState.update {
                    it.copy(
                        client = mc
                    )
                }
            }
        }
    }

    fun fetchuserInformation(uid: UserId): Deferred<User?> {
        return viewModelScope.async {
            accountsRepository.fetchUserInformation(uid).fold(
                onSuccess = { it },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message
                        )
                    }
                    null
                }
            )
        }
    }

    private suspend fun initializeChannelData(
        channelId: String,
        prevState: ServerChannelUiState
    ): ServerChannelUiState {
        val channelInfo = roomsRepository.roomDirectory.value[RoomId(channelId)]
            ?: accountsRepository.matrixClient?.room?.getById(RoomId(channelId))?.first()
            ?: return prevState.copy(error = "Uh oh! Cannot fetch channel info")

        Log.d("Channel View", "got channel info: $channelInfo")

        val messages = messagesRepository.fetchChannelMessages(
            channelId,
            limit = 50
        ) ?: return prevState.copy(error = "Uh oh! Cannot fetch messages for this channel")

        Log.d("Channel View", "got channel messages: ${messages.first().size}")

        return prevState.copy(
            channelId = channelId,
            channelInfo = channelInfo,
            messages = messages.stateIn(viewModelScope),
        )
    }

    fun updateMessage(new: String) {
        _uiState.value.draftMessage.update { new }
    }

    fun react(eventId: EventId, reactionKey: String, alreadyReacted: EventId?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (alreadyReacted == null) {
                accountsRepository.matrixClient?.room?.sendMessage(RoomId(uiState.value.channelId!!)) {
                    react(eventId, reactionKey)
                }
            } else {
                accountsRepository.matrixClient?.api?.room?.redactEvent(RoomId(uiState.value.channelId!!), alreadyReacted)
            }
        }
    }

    fun sendMessage() {
        _uiState.update {
            it.draftMessage.update { draftMessage ->
                viewModelScope.launch(Dispatchers.IO) {
                    if (it.channelId != null) {
                        val tree = parser.buildMarkdownTreeFromString(draftMessage)
                        val html = HtmlGenerator(draftMessage, tree, CommonMarkFlavourDescriptor()).generateHtml()
                            .replace(Regex("</?body>"), "") // remove default wrapping body tag
                            .replace(Regex("(^<p>|</p>$)"), "") // remove default wrapping paragraph

                        if (draftMessage.isNotBlank()) {
                            accountsRepository.matrixClient?.room?.sendMessage(RoomId(it.channelId)) {
                                val command = if (draftMessage.startsWith('/'))
                                    draftMessage.takeWhile { it != ' ' }
                                else null
                                when (command) {
                                    "/me" -> emote(formattedBody = html, format = "org.matrix.custom.html", body = draftMessage.removePrefix(command))
                                    "/notice" -> notice(formattedBody = html, format = "org.matrix.custom.html", body = draftMessage.removePrefix(command))
                                    else -> text(formattedBody = html, format = "org.matrix.custom.html", body = draftMessage)
                                }
                                when (val what = it.messageResponse) {
                                    is MessageResponse.Edit -> {
                                        replace(what.timelineEvent)
                                    }
                                    is MessageResponse.ReplyTo -> {
                                        reply(what.timelineEvent)
                                    }
                                }
                            }
                        }
                        it.files.forEach { (name, uri) ->
                            context.contentResolver.openInputStream(uri)?.buffered()?.use { it.readBytes() }?.let { bytes ->
                                accountsRepository.matrixClient?.room?.sendMessage(RoomId(it.channelId)) {
                                    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.split(".").last())
                                    if (mime?.startsWith("image/") == true) {
                                        image(
                                            body = name,
                                            fileName = name,
                                            image = flowOf(bytes),
                                            type = when (mime.split("/").last()) {
                                                "png" -> ContentType.Image.PNG
                                                "jpeg" -> ContentType.Image.JPEG
                                                "gif" -> ContentType.Image.GIF
                                                "svg" -> ContentType.Image.SVG
                                                else -> ContentType.Image.Any
                                            }
                                        )
                                    } else if (mime?.startsWith("audio/") == true) {
                                        audio(
                                            body = name,
                                            fileName = name,
                                            audio = flowOf(bytes),
                                            type = when (mime.split("/").last()) {
                                                "ogg" -> ContentType.Audio.OGG
                                                "mp4" -> ContentType.Audio.MP4
                                                "mpeg" -> ContentType.Audio.MPEG
                                                else -> ContentType.Audio.Any
                                            }
                                        )
                                    } else {
                                        file(
                                            body = name,
                                            fileName = name,
                                            file = flowOf(bytes)
                                        )
                                    }
                                }
                                Log.d("Channel View", "sent file ${uri.lastPathSegment?.split("/")?.last()}, url $uri, byte count: ${bytes.size}")
                            }
                        }
                    }
                }
                ""
            }
            it.copy(
                files = mutableMapOf(),
                messageResponse = null
            )
        }
    }

    fun addFile(name: String, uri: Uri) {
        _uiState.update {
            it.copy(
                files = it.files + (name to uri)
            )
        }
    }

    fun onAttachmentClick(uri: String) {

    }

    suspend fun goToBottom() {
        messageListState.scrollToItem(0)
    }

    suspend fun fetchEarlierMessages() {
        val firstMessage = uiState.value.messages?.value?.last()
        if (uiState.value.channelId != null && firstMessage != null) {
            messagesRepository.fetchChannelMessagesBefore(uiState.value.channelId!!, firstMessage)?.stateIn(viewModelScope)?.let { messages ->
                _uiState.update {
                    it.copy(
                        messages = messages
                    )
                }
            }
        }
    }

    fun onDialogDismiss() {
        _uiState.update {
            it.copy(
                isSendError = false,
                error = null
            )
        }
    }

    fun removeFiles(name: String) {
        _uiState.update {
            it.copy(
                files = it.files - name
            )
        }
    }

    fun deleteMessage(message: TimelineEvent) {
        viewModelScope.launch {
            accountsRepository.matrixClient?.api?.room?.redactEvent(
                roomId = RoomId(uiState.value.channelId!!),
                eventId = message.eventId,
                reason = "Deleted the message"
            )
        }
    }

    fun editMessage(message: TimelineEvent) {
        _uiState.update {
            it.draftMessage.update {
                when (val content = message.content?.getOrNull()) {
                    is RoomMessageEventContent -> {
                        content.body
                    }
                    else -> {
                        ""
                    }
                }
            }
            it.copy(
                messageResponse = MessageResponse.Edit(message)
            )
        }
    }

    fun replyMessage(message: TimelineEvent) {
        _uiState.update {
            it.copy(
                messageResponse = MessageResponse.ReplyTo(message)
            )
        }
    }

    fun unreplyMessage() {
        _uiState.update {
            it.copy(
                messageResponse = null
            )
        }
    }
}