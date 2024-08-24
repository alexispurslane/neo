package io.github.alexispurslane.bloc.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.AccountsRepository
import io.github.alexispurslane.bloc.ui.composables.screens.URL_REGEX
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            revoltAccountRepository.userSessionFlow.collectLatest {
                if (it != null) {
                    _uiState.update { prevState ->
                        prevState.copy(
                            instanceApiUrl = prevState.instanceApiUrl.ifEmpty {
                                it.instanceApiUrl
                            },
                            instanceUserName = prevState.instanceUserName.ifEmpty {
                                it.userId
                            },
                        )
                    }
                }
            }
        }
    }

    fun onInstanceInfoChange(apiUrl: String, email: String, pass: String) {
        _uiState.update { prevState ->
            prevState.copy(
                instanceApiUrl = apiUrl,
                instanceUserName = email,
                instancePassword = pass,
                urlValidated = apiUrl.matches(Regex(URL_REGEX))

            )
        }
    }

    fun onLogin() {
        viewModelScope.launch {
            if (uiState.value.urlValidated) {
                revoltAccountRepository.login(
                    uiState.value.instanceApiUrl,
                    uiState.value.instanceUserName,
                    uiState.value.instancePassword
                ).onFailure {
                    _uiState.update { prevState ->
                        Log.e("Login", it.stackTraceToString())
                        prevState.copy(
                            isLoginError = true,
                            loginErrorTitle = "Uh oh!",
                            loginErrorBody = it.message ?: "An unknown error has occurred"
                        )
                    }
                }
            } else {
                Log.w("Login ViewModel", "URL invalid")
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