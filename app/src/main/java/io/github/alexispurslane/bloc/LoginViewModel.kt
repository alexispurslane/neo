package io.github.alexispurslane.bloc

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.data.RevoltAccountsRepository
import io.github.alexispurslane.bloc.data.networking.models.LoginRequest
import io.github.alexispurslane.bloc.data.networking.models.LoginResponse
import io.github.alexispurslane.bloc.data.networking.models.MFALoginRequest
import io.github.alexispurslane.bloc.data.networking.models.MFAResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val instanceApiUrl: String = "",
    val instanceEmailAddress: String = "",
    val instancePassword: String = "",
    val urlValidated: Boolean = false,
    val urlValidationMessage: String = "",
    val mfa: Boolean = false,
    val mfaTicket: String = "",
    val mfaAllowedMethods: List<String> = emptyList(),
    val disabled: Boolean = false,
    val isLoginError: Boolean = false,
    val loginErrorTitle: String = "",
    val loginErrorBody: String = ""
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val revoltAccountRepository: RevoltAccountsRepository,
): ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val dataStoreUserSession = revoltAccountRepository.userSessionFlow.first()
            _uiState.update { prevState ->
                prevState.copy(
                    instanceApiUrl = if (prevState.instanceApiUrl.isEmpty()) dataStoreUserSession.instanceApiUrl ?: "" else prevState.instanceApiUrl,
                    instanceEmailAddress = if (prevState.instanceEmailAddress.isEmpty()) dataStoreUserSession.emailAddress ?: "" else prevState.instanceEmailAddress,
                )
            }
        }
    }

    fun onInstanceInfoChange(apiUrl: String, email: String, pass: String) {
        _uiState.update { prevState ->
            prevState.copy(
                instanceApiUrl = apiUrl,
                instanceEmailAddress = email,
                instancePassword = pass
            )
        }
    }

    fun onMultiFactorLoginConfirm(mfaMethod: String, mfaResponse: String, setLoggedIn: (Boolean) -> Unit) {
        viewModelScope.launch {
            val loginResponse = revoltAccountRepository.login(
                uiState.value.instanceApiUrl, MFALoginRequest(
                    mfa_ticket = uiState.value.mfaTicket,
                    friendly_name = "Bloc",
                    mfa_respose = when (mfaMethod) {
                        "Password" -> MFAResponse(password = mfaResponse, null, null)
                        "Recovery" -> MFAResponse(null, null, recovery_code = mfaResponse)
                        "Totp" -> MFAResponse(null, totp_code = mfaResponse, null)
                        else -> return@launch
                    }
                )
            )

            handleLoginResponse(loginResponse, setLoggedIn)
        }
    }

    fun onLogin(setLoggedIn: (Boolean) -> Unit) {
        viewModelScope.launch {
            val urlValidated = if (uiState.value.urlValidated) {
                true
            } else {
                testApiUrlSuspend()
                uiState.value.urlValidated
            }
            if (urlValidated) {
                val loginResponse = revoltAccountRepository.login(
                    uiState.value.instanceApiUrl,
                    uiState.value.instanceEmailAddress,
                    LoginRequest(
                        email = uiState.value.instanceEmailAddress,
                        password = uiState.value.instancePassword,
                        friendly_name = "Bloc"
                    )
                )
                handleLoginResponse(loginResponse, setLoggedIn)
            }
        }
    }

    private fun handleLoginResponse(loginResponse: Either<LoginResponse, String>, setLoggedIn: (Boolean) -> Unit) {
        _uiState.update { prevState ->
            when (loginResponse) {
                is Either.Success -> {
                    Log.d("SIGNIN SUCCESS", loginResponse.toString())
                    when (loginResponse.value.result) {
                        "Success" -> {
                            setLoggedIn(true)
                            prevState
                        }
                        "MFA" -> {
                            prevState.copy(
                                mfa = true,
                                mfaAllowedMethods = loginResponse.value.allowedMethods,
                                mfaTicket = loginResponse.value.ticket!!
                            )
                        }
                        "Disabled" -> {
                            prevState.copy(
                                disabled = true
                            )
                        }
                        else -> {
                            Log.d("SIGNIN SUCCESS", "Unknown login response result ${loginResponse.value.result}")
                            prevState
                        }
                    }
                }
                is Either.Error -> {
                    val (errorTitle, errorBody) = loginResponse.value.split(':')
                    prevState.copy(
                        isLoginError = true,
                        loginErrorTitle = errorTitle,
                        loginErrorBody = errorBody
                    )
                }
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

    private suspend fun validateUrl(instanceApiUrl: String): Pair<Boolean, String> {
        return try {
            val res = revoltAccountRepository.queryNode(instanceApiUrl)
            if (res.isSuccessful && res.body() != null) {
                Pair(true, "That looks like a Revolt v${res.body()!!.revolt} instance!")
            } else {
                Pair(false, "Uh oh! Got status code: ${res.message()}")
            }
        } catch (e: Exception) {
            Log.d("QUERY NODE ERROR", e.toString())
            Pair(false, "Uh oh! ${e.message}")
        }
    }

    private suspend fun testApiUrlSuspend() {
        _uiState.update { prevState ->
            val (success, statusMessage) = validateUrl(prevState.instanceApiUrl)
            prevState.copy(
                urlValidated = success,
                urlValidationMessage = statusMessage
            )
        }
    }
    fun testApiUrl() {
        viewModelScope.launch {
            testApiUrlSuspend()
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