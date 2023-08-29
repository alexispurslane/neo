package io.github.alexispurslane.bloc.ui.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.RevoltAccountsRepository
import io.github.alexispurslane.bloc.data.RevoltChannelsRepository
import io.github.alexispurslane.bloc.data.RevoltServersRepository
import io.github.alexispurslane.bloc.data.UserSession
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
    val channels: Map<String, RevoltChannel> = emptyMap()
)
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val revoltAccountRepository: RevoltAccountsRepository,
    private val revoltServersRepository: RevoltServersRepository,
    private val revoltChannelsRepository: RevoltChannelsRepository,
): ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            revoltAccountRepository.userSessionFlow.collect { userSession ->
                Log.d("USER HOME", userSession.toString())
                if (userSession.instanceApiUrl != null) {
                    if (userSession.sessionToken != null)
                        initializeSession(userSession)
                }
            }
        }

        viewModelScope.launch {
            revoltServersRepository.servers.collectLatest { servers ->
                _uiState.update { prevState ->
                    prevState.copy(
                        servers = servers
                    )
                }
            }
        }

        viewModelScope.launch {
            revoltChannelsRepository.channels.collectLatest { channels ->
                _uiState.update { prevState ->
                    prevState.copy(
                        channels = channels
                    )
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun initializeWebSockets(userSession: UserSession) = coroutineScope {
        if (RevoltWebSocketModule.setWebSocketUrlAndToken(userSession.websocketsUrl!!, userSession.sessionToken!!)) {
            // Initialize service (it's lazily created)
            RevoltWebSocketModule.service()
        }
    }
    private suspend fun initializeSession(userSession: UserSession) {
        RevoltApiModule.setBaseUrl(userSession.instanceApiUrl!!)
        when (val userInfo =
            revoltAccountRepository.fetchUserInformation()) {
            is Either.Success -> {
                Log.d(
                    "USER HOME",
                    "Successful fetch user info: ${userInfo.value.toString()}"
                )
                _uiState.update {
                    it.copy(userInfo = userInfo.value)
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
}