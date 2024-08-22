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
import io.github.alexispurslane.bloc.data.local.RevoltAutumnModule
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.data.network.models.RevoltServer
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userInfo: RevoltUser? = null,
    val servers: Map<String, RevoltServer> = emptyMap(),
    val channels: Map<String, RevoltChannel> = emptyMap(),
    val autumnUrl: String? = null,
    val lastServer: String? = null,
    val lastChannel: String? = null,
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
            revoltAccountRepository.userSessionFlow.collect { userSession ->
                Log.d("USER HOME", userSession.toString())
                if (userSession.instanceApiUrl != null) {
                    if (userSession.sessionToken != null) {
                        initializeSession(userSession)
                    }
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

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun initializeWebSockets(userSession: UserSession) =
        coroutineScope {
            if (RevoltWebSocketModule.setWebSocketUrlAndToken(
                    userSession.websocketsUrl!!,
                    userSession.sessionToken!!
                )
            ) {
                // Initialize service (it's lazily created)
                RevoltWebSocketModule.service()
            }
        }

    private suspend fun initializeSession(userSession: UserSession) {
        RevoltAutumnModule.setUrl(userSession.autumnUrl!!)
        RevoltApiModule.setBaseUrl(userSession.instanceApiUrl!!)
        when (val userInfo =
            revoltAccountRepository.fetchUserInformation()) {
            is Either.Success -> {
                _uiState.update {
                    val lastServer = userSession.preferences["last_server"]
                    val lastChannel =
                        lastServer?.let { userSession.preferences[it] }
                    it.copy(
                        userInfo = userInfo.value,
                        autumnUrl = userSession.autumnUrl,
                        lastServer = it.lastServer ?: lastServer,
                        lastChannel = it.lastChannel ?: lastChannel,
                        lastServerChannels = userSession.preferences
                    )
                }
            }

            is Either.Error -> {
                Log.d(
                    "USER HOME",
                    "Failed to get user profile: ${userInfo.value}"
                )
            }
        }

        initializeWebSockets(userSession)
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
                    "last_server" to currentServerId,
                    currentServerId to currentChannelId
                )
            )
        }
    }
}