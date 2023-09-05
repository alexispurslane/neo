package io.github.alexispurslane.bloc.viewmodels

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.RevoltAccountsRepository
import io.github.alexispurslane.bloc.data.RevoltChannelsRepository
import io.github.alexispurslane.bloc.data.RevoltServersRepository
import io.github.alexispurslane.bloc.data.UserSession
import io.github.alexispurslane.bloc.data.local.RevoltAutumnModule
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.data.network.models.RevoltServer
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userInfo: State<RevoltUser>? = null,
    val servers: SnapshotStateMap<String, RevoltServer> = mutableStateMapOf(),
    val channels: SnapshotStateMap<String, RevoltChannel> = mutableStateMapOf(),
    val autumnUrl: String? = null,
    val lastServer: String? = null,
    val lastChannel: String? = null,
    val lastServerChannels: Map<String, String> = emptyMap()
)

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val revoltAccountRepository: RevoltAccountsRepository,
    revoltServersRepository: RevoltServersRepository,
    revoltChannelsRepository: RevoltChannelsRepository,
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
                        Log.d(
                            "USER HOME",
                            "${revoltServersRepository.servers.size}, ${revoltChannelsRepository.channels.size}"
                        )
                        _uiState.update {
                            it.copy(
                                servers = revoltServersRepository.servers,
                                channels = revoltChannelsRepository.channels
                            )
                        }
                    }
                }
            }
        }
    }

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
        when (val userInfo = revoltAccountRepository.fetchUserInformation()) {
            is Either.Success -> {
                _uiState.update {
                    val lastServer = userSession.preferences["last_server"]
                    val lastChannel =
                        lastServer?.let { userSession.preferences[it] }
                    Log.d(
                        "HOME VIEW",
                        "last server: $lastServer, last channel: $lastChannel"
                    )
                    it.copy(
                        userInfo = userInfo.value,
                        autumnUrl = userSession.autumnUrl,
                        lastServer = lastServer ?: it.lastServer,
                        lastChannel = lastChannel ?: it.lastChannel,
                        lastServerChannels = userSession.preferences,
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
        Log.d(
            "HOME VIEW",
            "save preferences: $currentChannelId, $currentServerId"
        )
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