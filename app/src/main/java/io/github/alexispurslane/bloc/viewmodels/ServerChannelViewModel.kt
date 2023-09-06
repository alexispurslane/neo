package io.github.alexispurslane.bloc.viewmodels

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.RevoltAccountsRepository
import io.github.alexispurslane.bloc.data.RevoltChannelsRepository
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
)

@HiltViewModel
class ServerChannelViewModel @Inject constructor(
    private val revoltAccountsRepository: RevoltAccountsRepository,
    private val revoltServersRepository: RevoltServersRepository,
    private val revoltChannelsRepository: RevoltChannelsRepository,
    private val revoltMessagesRepository: RevoltMessagesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerChannelUiState())
    val uiState: StateFlow<ServerChannelUiState> = _uiState.asStateFlow()

    val messageListState = LazyListState()

    init {
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

    fun updateMessage(new: String) {
        _uiState.update {
            it.copy(draftMessage = new)
        }
    }

    fun sendMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.value.channelId != null && uiState.value.draftMessage.isNotBlank()) {
                val content = uiState.value.draftMessage
                val message = RevoltMessageSent(
                    content = content,
                    attachments = null,
                    replyIds = null,
                    embeds = null,
                    masquerade = null,
                    interactions = null
                )
                val res = revoltMessagesRepository.sendMessage(
                    uiState.value.channelId!!,
                    message
                )
                when (res) {
                    is Either.Success -> {
                        Log.d("CHANNEL VIEW", "Sent message: ${res.value}")
                        _uiState.update {
                            it.copy(
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