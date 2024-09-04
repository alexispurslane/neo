package io.github.alexispurslane.neo.viewmodels

import android.content.Intent
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.neo.MainApplication
import io.github.alexispurslane.neo.data.AccountsRepository
import io.github.alexispurslane.neo.data.models.User
import io.github.alexispurslane.service.Actions
import io.github.alexispurslane.service.NotificationService
import io.github.alexispurslane.service.ServiceState
import io.github.alexispurslane.service.getServiceState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.core.model.UserId
import javax.inject.Inject

data class UserProfileUiState(
    val editing: Boolean = false,
    val currentUserId: String? = null,
    val userProfile: User? = null,
    val serviceOn: Boolean = false,
    val isMyProfile: Boolean = false,
    val client: MatrixClient? = null,
    val preferences: Map<String, String> = mapOf()
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val savedStateHandle: SavedStateHandle,
    private val application: MainApplication,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountsRepository.matrixClientFlow.collectLatest { matrixClient ->
                _uiState.update {
                    it.copy(
                        client = matrixClient
                    )
                }
            }
        }

        viewModelScope.launch {
            savedStateHandle.getStateFlow("userId", "@me").collectLatest { userId ->
                accountsRepository.matrixClientFlow.collectLatest { matrixClient ->
                    accountsRepository.userSessionFlow.collectLatest { userSession ->
                        if (userSession != null) {
                            initializeUserProfile(
                                userId == "@me",
                                if (userId == "@me") accountsRepository.userId(userSession.userId, userSession.instanceApiUrl) else UserId(userId),
                                userSession.preferences
                            )
                        }
                    }
                }
            }
        }


        updateServiceState()
    }

    fun updateServiceState() {
        _uiState.update {
            it.copy(serviceOn = getServiceState(application.applicationContext) == ServiceState.STARTED)
        }
    }

    fun toggleNotificationsService(value: Boolean) {
        if (!value && getServiceState(application.applicationContext) == ServiceState.STOPPED) return
        Intent(
            application.applicationContext,
            NotificationService::class.java
        ).apply {
            action = if (value) Actions.START.name else Actions.STOP.name
            application.startForegroundService(this)
        }
        _uiState.update {
            it.copy(serviceOn = value)
        }
    }

    fun logout() {
        viewModelScope.launch {
            accountsRepository.logout()
        }
    }

    private suspend fun initializeUserProfile(isOwnProfile: Boolean, userId: UserId, preferences: Map<String, String>) =
        coroutineScope {
            accountsRepository.fetchUserInformation(userId).onSuccess { userProfile ->
                Log.d(
                    "USER PROFILE",
                    "${userId} display name: ${userProfile.displayName}, is own profile: $isOwnProfile"
                )
                Log.d("User Profile", "")
                _uiState.update {
                    it.copy(
                        isMyProfile = isOwnProfile,
                        currentUserId = userId.full,
                        userProfile = userProfile,
                        preferences = preferences
                    )
                }
            }.onFailure {
                Log.d(
                    "USER PROFILE",
                    "Failed to get user profile: $it"
                )
            }
        }

    fun setPreference(key: String, value: Any) {
        viewModelScope.launch {
            accountsRepository.savePreferences(
                mapOf(key to value.toString())
            )
        }
    }
}