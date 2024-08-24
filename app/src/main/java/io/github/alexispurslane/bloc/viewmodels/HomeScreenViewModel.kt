package io.github.alexispurslane.bloc.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.MainApplication
import io.github.alexispurslane.bloc.data.AccountsRepository
import io.github.alexispurslane.bloc.data.RoomTree
import io.github.alexispurslane.bloc.data.RoomsRepository
import io.github.alexispurslane.bloc.data.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import javax.inject.Inject

data class HomeUiState(
    val userInfo: User? = null,
    val lastServer: String? = null,
    val lastChannel: String? = null,
    val lastServerChannels: Map<String, String> = emptyMap(),
    val client: MatrixClient? = null
)

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val roomsRepository: RoomsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val rooms
        get() = roomsRepository.rooms

    init {
        viewModelScope.launch {
            accountsRepository.matrixClientFlow.collectLatest { mc ->
                mc?.loginState?.collectLatest { loginState ->
                    if (loginState == MatrixClient.LoginState.LOGGED_IN) {
                        accountsRepository.userSessionFlow.collectLatest { userSession ->
                            if (userSession != null) {
                                Log.d("Home Screen", userSession.toString())
                                accountsRepository.fetchUserInformation(accountsRepository.userId(userSession.userId, userSession.instanceApiUrl)).onSuccess { userInfo ->
                                    _uiState.update {
                                        val lastServer = userSession.preferences["lastServer"]
                                        val lastChannel =
                                            lastServer?.let { userSession.preferences[it] }
                                        it.copy(
                                            client = mc,
                                            userInfo = userInfo,
                                            lastServer = it.lastServer ?: lastServer,
                                            lastChannel = it.lastChannel ?: lastChannel,
                                            lastServerChannels = userSession.preferences,
                                        )
                                    }
                                }.onFailure {
                                    Log.e("Home Screen", it.stackTraceToString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun saveLast(currentServerId: String, currentChannelId: String) {
        viewModelScope.launch {
            accountsRepository.savePreferences(
                mapOf(
                    "lastServer" to currentServerId,
                    currentServerId to currentChannelId
                )
            )
        }
    }
}