package io.github.alexispurslane.bloc.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.data.AccountsRepository
import io.github.alexispurslane.bloc.data.MessagesRepository
import io.github.alexispurslane.bloc.data.RoomTree
import io.github.alexispurslane.bloc.data.RoomsRepository
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.RoomId
import javax.inject.Inject

data class ServerChannelUiState(
    val channelId: String? = null,
    val channelInfo: Room? = null,
    val serverInfo: Room? = null,
    val currentUserId: String? = null,
    val client: MatrixClient? = null,
    val error: String? = null,
    val atBeginning: Boolean = false,
    val newMessages: Boolean = false,
    val draftMessage: String = "",
    val isSendError: Boolean = false,
    val sendErrorTitle: String = "",
    val sendErrorText: String = "",
    val messages: StateFlow<List<TimelineEvent>>? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ServerChannelViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val roomsRepository: RoomsRepository,
    private val messagesRepository: MessagesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerChannelUiState())
    val uiState: StateFlow<ServerChannelUiState> = _uiState.asStateFlow()

    val users
        get() = accountsRepository.users

    val messageListState = LazyListState()

    init {
        viewModelScope.launch {
            accountsRepository.userSessionFlow.collect {
                if (it?.userId != null) {
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
                            initializeChannelData(savedStateHandle["serverId"]!!, channelId, it)
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

    fun updateMessage(new: String) {
        _uiState.update {
            it.copy(draftMessage = new)
        }
    }

    fun sendMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.value.channelId != null && uiState.value.draftMessage.isNotBlank()) {
                accountsRepository?.matrixClient?.room?.sendMessage(RoomId(uiState.value.channelId!!)) {
                    text(uiState.value.draftMessage)
                }
            }
        }
    }

    suspend fun goToBottom() {
        messageListState.scrollToItem(0)
        _uiState.update { it.copy(newMessages = false) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun initializeChannelData(
        spaceId: String,
        channelId: String,
        prevState: ServerChannelUiState
    ): ServerChannelUiState {
        val serverInfo = roomsRepository.rooms.value[RoomId(spaceId)] as? RoomTree.Space?
        val channelInfo: RoomTree.Channel = if (spaceId == "@me") {
            roomsRepository.rooms.value[RoomId(channelId)] as? RoomTree.Channel?
        } else {
            serverInfo!!.children[RoomId(channelId)] as? RoomTree.Channel?
        }
            ?: return prevState.copy(error = "Uh oh! Cannot fetch channel info")

        val members =
            roomsRepository.fetchSpaceMembers(serverInfo?.space?.roomId ?: channelInfo.room.roomId)?.lastOrNull()
                ?: return prevState.copy(error = "Uh oh! Cannot fetch member info for channel")

        val users = members
            .associateBy { user ->
                user.userId
            }

        val messages = messagesRepository.channelMessages[RoomId(channelId)]
            ?: messagesRepository.fetchChannelMessages(
                channelId,
                limit = 50
            ) ?: return prevState.copy(error = "Uh oh! Cannot fetch messages for this channel")

        accountsRepository.prefetchUsersForChannel(RoomId(channelId))

        return prevState.copy(
            channelId = channelId,
            channelInfo = channelInfo.room,
            serverInfo = serverInfo?.space,
            messages = messages.stateIn(viewModelScope)
        )
    }

    suspend fun fetchEarlierMessages() {
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