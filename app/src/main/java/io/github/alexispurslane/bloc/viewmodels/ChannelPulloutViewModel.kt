package io.github.alexispurslane.bloc.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.RevoltChannelsRepository
import io.github.alexispurslane.bloc.data.RevoltServersRepository
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.data.network.models.RevoltServer
import io.github.alexispurslane.bloc.data.network.models.RevoltServerMember
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ChannelPulloutUiState(
    val channelId: String? = null,
    val channelInfo: State<RevoltChannel?> = mutableStateOf(null),
    val serverInfo: State<RevoltServer?> = mutableStateOf(null),
    val users: List<Pair<RevoltUser, RevoltServerMember>> = emptyList()
)

@HiltViewModel
class ChannelPulloutViewModel @Inject constructor(
    private val revoltChannelsRepository: RevoltChannelsRepository,
    private val revoltServersRepository: RevoltServersRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChannelPulloutUiState())
    val uiState = _uiState.asStateFlow()

    suspend fun setChannelId(id: String) {
        _uiState.update {
            it.copy(
                channelId = id
            )
        }
        initializeContents(id)
    }

    private suspend fun initializeContents(id: String) {
        val channelInfo =
            derivedStateOf { revoltChannelsRepository.channels[id] }
        if (channelInfo.value == null) return
        if (channelInfo.value!! is RevoltChannel.TextChannel) {
            val channel = channelInfo.value!! as RevoltChannel.TextChannel
            val serverInfo =
                derivedStateOf { revoltServersRepository.servers[channel.serverId] }
            if (serverInfo.value == null) return
            val membersInfo =
                revoltServersRepository.fetchServerMembers(channel.serverId)
            if (membersInfo is Either.Error) return
            val members = (membersInfo as Either.Success).value
            _uiState.update {
                it.copy(
                    serverInfo = serverInfo,
                    channelInfo = channelInfo,
                    users = members.users.zip(members.members)
                )
            }
        }
    }
}