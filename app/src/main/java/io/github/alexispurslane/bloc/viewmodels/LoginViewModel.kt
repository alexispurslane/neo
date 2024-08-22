package io.github.alexispurslane.bloc.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.AccountsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import javax.inject.Inject

data class LoginUiState(
    val instanceApiUrl: String = "",
    val instanceUserName: String = "",
    val instancePassword: String = "",
    val urlValidated: Boolean = false,
    val urlValidationMessage: String = "",
    val mfa: Boolean = false,
    val mfaTicket: String = "",
    val mfaAllowedMethods: List<String> = emptyList(),
    val isLoginError: Boolean = false,
    val loginErrorTitle: String = "",
    val loginErrorBody: String = "",
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val revoltAccountRepository: AccountsRepository,
) : ViewModel() {
    private var websocketUrl: String? = null
    private var autumnUrl: String? = null
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userSession = revoltAccountRepository.userSessionFlow.first()
            _uiState.update { prevState ->
                prevState.copy(
                    instanceApiUrl = if (prevState.instanceApiUrl.isEmpty()) userSession.instanceApiUrl
                        ?: "" else prevState.instanceApiUrl,
                    instanceUserName = if (prevState.instanceUserName.isEmpty()) userSession.userId
                        ?: "" else prevState.instanceUserName,
                )
            }
        }
    }

    fun onInstanceInfoChange(apiUrl: String, email: String, pass: String) {
        _uiState.update { prevState ->
            prevState.copy(
                instanceApiUrl = apiUrl,
                instanceUserName = email,
                instancePassword = pass
            )
        }
    }

    fun onMultiFactorLoginConfirm(
        mfaMethod: String,
        mfaResponse: String,
        setLoggedIn: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val loginResponse = revoltAccountRepository.login(
                uiState.value.instanceApiUrl,
                uiState.value.instanceUserName
            )

            handleLoginResponse(loginResponse, setLoggedIn)
        }
    }

    fun onLogin(setLoggedIn: (Boolean) -> Unit) {
        viewModelScope.launch {
            val urlValidated = if (uiState.value.urlValidated) {
                true
            } else {
                uiState.value.urlValidated
            }
            if (urlValidated) {
                val loginResponse = revoltAccountRepository.login(
                    uiState.value.instanceApiUrl,
                    uiState.value.instanceUserName,
                )
                handleLoginResponse(loginResponse, setLoggedIn)
            }
        }
    }

    private fun handleLoginResponse(
        loginResponse: Boolean,
        setLoggedIn: (Boolean) -> Unit
    ) {
        _uiState.update { prevState ->
            if (loginResponse) {
                setLoggedIn(true)
                prevState
            } else {
                prevState.copy(
                    isLoginError = true,
                    loginErrorTitle = "Uh oh!",
                    loginErrorBody = "Unknown login error occurred"
                )
            }
        }
    }

    fun onLoginErrorDismiss() {
        _uiState.update { prevState ->
            prevState.copy(
                isLoginError = false,
            )
        }
    }

    fun onBack() {
        _uiState.update { prevState ->
            prevState.copy(
                mfa = false,
                mfaTicket = "",
                mfaAllowedMethods = emptyList()
            )
        }
    }
}