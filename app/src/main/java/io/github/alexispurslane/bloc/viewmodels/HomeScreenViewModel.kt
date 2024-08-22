package io.github.alexispurslane.bloc.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.MainApplication
import io.github.alexispurslane.bloc.data.AccountsRepository
import io.github.alexispurslane.bloc.data.ChannelsRepository
import io.github.alexispurslane.bloc.data.ServersRepository
import io.github.alexispurslane.bloc.data.UserSession
import io.github.alexispurslane.bloc.data.models.User
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import javax.inject.Inject

data class HomeUiState(
    val userInfo: User? = null,
    val lastServer: String? = null,
    val lastChannel: String? = null,
    val servers: Map<RoomId, Room> = emptyMap(),
    val channels: Map<RoomId, Map<RoomId, Room>> = emptyMap(),
    val lastServerChannels: Map<String, String> = emptyMap()
)

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val revoltAccountRepository: AccountsRepository,
    private val serversRepository: ServersRepository,
    private val channelsRepository: ChannelsRepository,
    private val application: MainApplication
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            revoltAccountRepository.matrixClient?.loginState?.collect { loginState ->
                if (loginState == MatrixClient.LoginState.LOGGED_IN) {
                    initializeSession(revoltAccountRepository.userSessionFlow.first())
                }
            }
        }

        viewModelScope.launch {
            serversRepository.spaces.collectLatest { servers ->
                _uiState.update { prevState ->
                    prevState.copy(
                        servers = servers
                    )
                }
            }
        }

        viewModelScope.launch {
            channelsRepository.channels.collectLatest { channels ->
                _uiState.update { prevState ->
                    prevState.copy(
                        channels = channels
                    )
                }
            }
        }
    }

    private suspend fun initializeSession(userSession: UserSession) {
        revoltAccountRepository.fetchUserInformation(UserId(userSession.userId)).onSuccess { userInfo ->
            _uiState.update {
                val lastServer = userSession.preferences["lastServer"]
                val lastChannel =
                    lastServer?.let { userSession.preferences[it] }
                it.copy(
                    userInfo = userInfo,
                    lastServer = it.lastServer ?: lastServer,
                    lastChannel = it.lastChannel ?: lastChannel,
                    lastServerChannels = userSession.preferences
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            revoltAccountRepository.clearSession()
        }
    }

    fun saveLast(currentServerId: String, currentChannelId: String) {
        viewModelScope.launch {
            revoltAccountRepository.savePreferences(
                mapOf(
                    "lastServer" to currentServerId,
                    currentServerId to currentChannelId
                )
            )
        }
    }
}