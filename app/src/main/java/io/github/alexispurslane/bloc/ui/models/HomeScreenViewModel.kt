package io.github.alexispurslane.bloc.ui.models

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.RevoltAccountsRepository
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketRequest
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userInfo: RevoltUser? = null
)
@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val revoltAccountRepository: RevoltAccountsRepository,
): ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            revoltAccountRepository.userSessionFlow.collect { userSession ->
                Log.d("USER HOME", userSession.toString())
                if (userSession.instanceApiUrl != null) {
                    RevoltApiModule.setBaseUrl(userSession.instanceApiUrl)
                    if (userSession.sessionToken != null) {
                        val res = revoltAccountRepository.queryNode(userSession.instanceApiUrl)
                        if (res.isSuccessful && res.body() != null) {
                            if (RevoltWebSocketModule.setWebSocketUrl(res.body()!!.ws)) {
                                val event = RevoltWebSocketModule.service().observeOnConnectionOpenedEvent().consumeAsFlow().first()
                                val req = RevoltWebSocketRequest.Authenticate(sessionToken = userSession.sessionToken)
                                val mapper = ObjectMapper()
                                Log.d("USER HOME", "Sending websocket Authenticate message: ${mapper.writeValueAsString(req)}")
                                RevoltWebSocketModule.service().send(req)
                                RevoltWebSocketModule.service()
                                    .observeEvent().consumeAsFlow().collect {
                                        when (it) {
                                            is RevoltWebSocketResponse.Authenticated -> {
                                                Log.d("USER HOME", "websocket authenticated")
                                            }
                                            else -> {
                                                Log.d("USER HOME", it.toString())
                                            }
                                        }
                                    }
                            }
                        } else {
                            Log.e("USER HOME", "Unable to query API node")
                        }

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
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            revoltAccountRepository.clearSession()
        }
    }
}
