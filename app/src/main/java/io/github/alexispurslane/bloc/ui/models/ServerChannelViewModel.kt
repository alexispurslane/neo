package io.github.alexispurslane.bloc.ui.models

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.RevoltAccountsRepository
import io.github.alexispurslane.bloc.data.RevoltChannelsRepository
import io.github.alexispurslane.bloc.data.RevoltMessagesRepository
import io.github.alexispurslane.bloc.data.RevoltServersRepository
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.data.network.models.RevoltMessage
import io.github.alexispurslane.bloc.data.network.models.RevoltServer
import io.github.alexispurslane.bloc.data.network.models.RevoltServerMember
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServerChannelUiState(
    val channelId: String? = null,
    val channelInfo: RevoltChannel? = null,
    val serverInfo: RevoltServer? = null,
    val messages: List<RevoltMessage> = emptyList(),
    val users: Map<String, Pair<RevoltUser, RevoltServerMember>> = emptyMap(),
    val currentUserId: String? = null,
)
@HiltViewModel
class ServerChannelViewModel @Inject constructor(
    private val revoltAccountsRepository: RevoltAccountsRepository,
    private val revoltServersRepository: RevoltServersRepository,
    private val revoltChannelsRepository: RevoltChannelsRepository,
    private val revoltMessagesRepository: RevoltMessagesRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {

    private val _uiState = MutableStateFlow(ServerChannelUiState())
    val uiState: StateFlow<ServerChannelUiState> = _uiState.asStateFlow()

    private val regex by lazy { Regex("<@([a-zA-Z0-9]+)>") }

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

        // Whenever the current channel changes, get the initial dump of messages
        viewModelScope.launch {
            savedStateHandle.getStateFlow("channelId", null)
                .collect { channelId: String? ->
                    if (channelId != null) {
                        _uiState.update {
                            fetchInitialMessages(channelId, it)
                        }
                    }
                }
        }

        // When a new message is added, add it.
        //
        // NOTE: These are separate stages, instead of having a continuous
        // SharedFlow<List<Message>>, for performance reasons, so the whole
        // list doesn't have to get re-built over and over and over again
        viewModelScope.launch {
            revoltMessagesRepository.messages.collect { newMessage ->
                Log.d("CHANNEL VIEW", newMessage.toString())
                if (newMessage.channelId == uiState.value.channelId) {
                    Log.d(
                        "CHANNEL VIEW",
                        "Correct ID: ${newMessage.channelId} == ${uiState.value.channelId}"
                    )
                    Log.d(
                        "CHANNEL VIEW",
                        "message count: ${uiState.value.messages.size}"
                    )
                    val message = newMessage.copy(
                        content = newMessage.content?.replace(regex) { matchResult: MatchResult ->
                            uiState.value.users[matchResult.groupValues[0]]?.first?.displayName
                                ?: "unknown_user"
                        }
                    )
                    _uiState.update { prevState ->
                        prevState.copy(
                            messages = prevState.messages.plus(message)
                        )
                    }
                    Log.d(
                        "CHANNEL VIEW",
                        "message count: ${uiState.value.messages.size}"
                    )
                }
            }
        }
    }

    private suspend fun fetchInitialMessages(
        channelId: String,
        it: ServerChannelUiState
    ): ServerChannelUiState {
        when (val channelInfo =
            revoltChannelsRepository.channels.value[channelId]) {
            is RevoltChannel.TextChannel -> {
                val serverInfo =
                    revoltServersRepository.servers.value[channelInfo.serverId]
                if (serverInfo != null) {
                    when (val messages =
                        revoltMessagesRepository.fetchMessages(
                            channelId,
                            limit = 50,
                        )) {
                        is Either.Success -> {
                            val users = when (val membersInfo =
                                revoltServersRepository.fetchServerMembers(
                                    serverInfo.serverId
                                )) {
                                is Either.Success -> {
                                    membersInfo.value.users.zip(membersInfo.value.members)
                                        .associate { (user, member) ->
                                            Log.d(
                                                "CHANNEL VIEW",
                                                "Found user ${user.userId}, @${user.userName}#${user.discriminator}"
                                            )
                                            user.userId to (user to member)
                                        }
                                }

                                is Either.Error -> {
                                    Log.e(
                                        "CHANNEL VIEW",
                                        "Unable to retrieve server member list: ${membersInfo.value}"
                                    )
                                    it.users
                                }
                            }
                            return it.copy(
                                channelId = channelId,
                                channelInfo = channelInfo,
                                serverInfo = serverInfo,
                                messages = messages.value.map {
                                    it.copy(
                                        content = it.content?.replace(regex) { matchResult: MatchResult ->
                                            Log.d(
                                                "CHANNEL VIEW",
                                                matchResult.groupValues[1]
                                            )
                                            "@" + (users.get(matchResult.groupValues[1])?.first?.userName
                                                ?: "unknown_user")
                                        }
                                    )
                                },
                                users = users
                            )
                        }

                        is Either.Error -> {
                            Log.e(
                                "CHANNEL VIEW",
                                messages.value
                            )
                        }
                    }
                }
            }

            else -> {}
        }
        return it
    }
}
