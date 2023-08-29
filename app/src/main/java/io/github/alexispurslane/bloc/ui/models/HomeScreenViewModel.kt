package io.github.alexispurslane.bloc.ui.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.MainApplication
import io.github.alexispurslane.bloc.data.RevoltAccountsRepository
import io.github.alexispurslane.bloc.data.RevoltServersRepository
import io.github.alexispurslane.bloc.data.UserSession
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltErrorId
import io.github.alexispurslane.bloc.data.network.models.RevoltServer
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketRequest
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class HomeUiState(
    val userInfo: RevoltUser? = null,
    val servers: List<RevoltServer> = emptyList()
)
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val revoltAccountRepository: RevoltAccountsRepository,
    private val revoltServersRepository: RevoltServersRepository,
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
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun initializeWebSockets(userSession: UserSession) = coroutineScope {
        if (RevoltWebSocketModule.setWebSocketUrlAndToken(userSession.websocketsUrl!!, userSession.sessionToken!!)) {
            RevoltWebSocketModule.service() // initialize service (it's lazily created)
            // Subscribe things to the websockets
            RevoltWebSocketModule.subscribe(revoltAccountRepository::onWebSocketEvent)
            RevoltWebSocketModule.subscribe(revoltServersRepository::onWebSocketEvent)
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