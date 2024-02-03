package io.github.alexispurslane.bloc.viewmodels

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.RevoltAccountsRepository
import io.github.alexispurslane.bloc.data.RevoltChannelsRepository
import io.github.alexispurslane.bloc.data.RevoltEmojiRepository
import io.github.alexispurslane.bloc.data.RevoltMessagesRepository
import io.github.alexispurslane.bloc.data.RevoltServersRepository
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.data.network.models.RevoltMessage
import io.github.alexispurslane.bloc.data.network.models.RevoltMessageSent
import io.github.alexispurslane.bloc.data.network.models.RevoltServer
import io.github.alexispurslane.bloc.data.network.models.RevoltServerMember
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerChannelUiState(
    val channelId: String? = null,
    val channelInfo: RevoltChannel? = null,
    val serverInfo: RevoltServer? = null,
    val users: Map<String, Pair<RevoltUser, RevoltServerMember>> = emptyMap(),
    val messages: SnapshotStateList<RevoltMessage> = mutableStateListOf(),
    val currentUserId: String? = null,
    val error: String? = null,
    val atBeginning: Boolean = false,
    val newMessages: Boolean = false,
    val draftMessage: String = "",
    val isSendError: Boolean = false,
    val sendErrorTitle: String = "",
    val sendErrorText: String = "",
    val selectedMessage: RevoltMessage? = null,
    val editingMessage: RevoltMessage? = null,
    val serverEmoji: List<String> = listOf()
)

@HiltViewModel
class ServerChannelViewModel @Inject constructor(
    private val revoltAccountsRepository: RevoltAccountsRepository,
    private val revoltServersRepository: RevoltServersRepository,
    private val revoltChannelsRepository: RevoltChannelsRepository,
    private val revoltMessagesRepository: RevoltMessagesRepository,
    private val revoltEmojiRepository: RevoltEmojiRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerChannelUiState())
    val uiState: StateFlow<ServerChannelUiState> = _uiState.asStateFlow()

    val messageListState = LazyListState()

    init {
        viewModelScope.launch {
            snapshotFlow {
                val emojis = revoltEmojiRepository.emojiIds.keys.toList()
                _uiState.update {
                    it.copy(
                        serverEmoji = emojis
                    )
                }
            }
        }
        viewModelScope.launch {
            revoltAccountsRepository.userSessionFlow.collect {
                if (it.userId != null) {
                    _uiState.update { prevState ->
                        prevState.copy(
                            currentUserId = it.userId
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            savedStateHandle.getStateFlow("channelId", null)
                .collectLatest { channelId: String? ->
                    if (channelId != null) {
                        _uiState.update {
                            initializeChannelData(channelId, it)
                        }
                    }
                }
        }

        viewModelScope.launch {
            RevoltWebSocketModule.eventFlow.collectLatest { event ->
                when (event) {
                    is RevoltWebSocketResponse.Message -> {
                        if (event.message.channelId == uiState.value.channelId) {
                            if (messageListState.firstVisibleItemIndex < 20) {
                                messageListState.scrollToItem(0)
                                _uiState.update {
                                    it.copy(newMessages = false)
                                }
                            } else {
                                _uiState.update {
                                    it.copy(newMessages = true)
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    fun handleMessageClick(messageId: String) {
        _uiState.update {
            it.copy(
                selectedMessage = it.messages.find { it.messageId == messageId }
            )
        }
    }

    fun handleMessageClose() {
        _uiState.update {
            it.copy(
                selectedMessage = null
            )
        }
    }

    fun handleDeleteMessage() {
        if (uiState.value.selectedMessage != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val res = revoltMessagesRepository.deleteMessage(
                    uiState.value.channelId!!,
                    uiState.value.selectedMessage!!.messageId
                )
                when (res) {
                    is Either.Success -> {
                        _uiState.update { it.copy(selectedMessage = null) }
                        Log.d("CHANNEL VIEW", "Deleted message")
                    }

                    is Either.Error -> {
                        val split = res.value.split(':')
                        _uiState.update {
                            it.copy(
                                isSendError = true,
                                sendErrorTitle = split[0],
                                sendErrorText = split[1]
                            )
                        }
                    }
                }
            }

        }
    }

    fun handleReactClick(
        messageId: String,
        emojiUrlId: String,
        isUsersReaction: Boolean
    ) {
        val split = emojiUrlId.split(":")
        val emojiId = split.last()
        if (!isUsersReaction) {
            viewModelScope.launch(Dispatchers.IO) {
                val res = revoltMessagesRepository.addReaction(
                    uiState.value.channelId!!,
                    messageId,
                    emojiId
                )
                when (res) {
                    is Either.Success -> {
                        Log.d("CHANNEL VIEW", "Sent react: $emojiId")
                    }

                    is Either.Error -> {
                        val split = res.value.split(':')
                        _uiState.update {
                            it.copy(
                                isSendError = true,
                                sendErrorTitle = split[0],
                                sendErrorText = split[1]
                            )
                        }
                    }
                }
            }
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val res = revoltMessagesRepository.removeReaction(
                    uiState.value.channelId!!,
                    messageId,
                    emojiId,
                    uiState.value.currentUserId
                )
                when (res) {
                    is Either.Success -> {
                        Log.d("CHANNEL VIEW", "Sent react: $emojiId")
                    }

                    is Either.Error -> {
                        val split = res.value.split(':')
                        _uiState.update {
                            it.copy(
                                isSendError = true,
                                sendErrorTitle = split[0],
                                sendErrorText = split[1]
                            )
                        }
                    }
                }
            }
        }
    }

    fun handleEditMessage() {
        if (uiState.value.selectedMessage != null) {
            _uiState.update {
                it.copy(
                    editingMessage = it.selectedMessage,
                    selectedMessage = null,
                    draftMessage = uiState.value.selectedMessage!!.content
                        ?: ""
                )
            }
        }
    }

    fun updateMessage(new: String) {
        _uiState.update {
            it.copy(draftMessage = new)
        }
    }

    fun sendMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.value.channelId != null && uiState.value.draftMessage.isNotBlank()) {
                val res = if (uiState.value.editingMessage != null) {
                    revoltMessagesRepository.editMessage(
                        uiState.value.channelId!!,
                        uiState.value.editingMessage!!.messageId,
                        uiState.value.draftMessage,
                        listOf()
                    )
                } else {
                    val content = uiState.value.draftMessage
                    val message = RevoltMessageSent(
                        content = content,
                        attachments = null,
                        replyIds = null,
                        embeds = null,
                        masquerade = null,
                        interactions = null
                    )
                    revoltMessagesRepository.sendMessage(
                        uiState.value.channelId!!,
                        message
                    )
                }
                when (res) {
                    is Either.Success -> {
                        Log.d("CHANNEL VIEW", "Sent message: ${res.value}")
                        _uiState.update {
                            it.copy(
                                editingMessage = null,
                                draftMessage = ""
                            )
                        }
                    }

                    is Either.Error -> {
                        val split = res.value.split(':')
                        _uiState.update {
                            it.copy(
                                isSendError = true,
                                sendErrorTitle = split[0],
                                sendErrorText = split[1]
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun goToBottom() {
        messageListState.scrollToItem(0)
        _uiState.update { it.copy(newMessages = false) }
    }

    private suspend fun initializeChannelData(
        channelId: String,
        prevState: ServerChannelUiState
    ): ServerChannelUiState {
        val channelInfo = revoltChannelsRepository.channels[channelId]
        if (channelInfo !is RevoltChannel.TextChannel) return prevState.copy(
            error = "Uh oh! Unable to locate channel"
        )

        val serverInfo =
            revoltServersRepository.servers[channelInfo.serverId]
        if (serverInfo == null) return prevState.copy(error = "Uh oh! Unable to locate server")

        val membersInfo =
            revoltServersRepository.fetchServerMembers(serverInfo.serverId)
        if (membersInfo is Either.Error) return prevState.copy(error = membersInfo.value)

        val members = membersInfo as Either.Success
        val users = members.value.users.zip(members.value.members)
            .associate { (user, member) ->
                user.userId to (user to member)
            }

        val messages = revoltMessagesRepository.fetchChannelMessages(
            channelId,
            limit = 50
        )
        if (messages is Either.Error) return prevState.copy(error = messages.value)

        return prevState.copy(
            channelId = channelId,
            channelInfo = channelInfo,
            serverInfo = serverInfo,
            users = users,
            messages = (messages as Either.Success).value
        )
    }

    fun fetchEarlierMessages() {
        viewModelScope.launch(Dispatchers.Default) {
            val len = uiState.value.messages.size
            Log.d(
                "CHANNEL VIEW",
                "Fetching earlier messages (current message count: $len)"
            )
            val last = uiState.value.messages.lastOrNull()
            if (uiState.value.channelId != null && last != null) {
                revoltMessagesRepository.fetchChannelMessages(
                    uiState.value.channelId!!,
                    limit = 50,
                    before = last.messageId
                )
            }
            val lenAfter = uiState.value.messages.size
            if (lenAfter - len < 49) {
                _uiState.update {
                    it.copy(
                        atBeginning = true
                    )
                }
            }
            Log.d("CHANNEL VIEW", "post-request message count: $len")
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
}