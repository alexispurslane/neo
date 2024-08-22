package io.github.alexispurslane.bloc.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.AccountsRepository
import io.github.alexispurslane.bloc.data.models.User
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.UserId
import javax.inject.Inject

data class UserProfileUiState(
    val editing: Boolean = false,
    val currentUserId: String? = null,
    val userProfile: User? = null
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            savedStateHandle.getStateFlow("userId", null)
                .collectLatest { userId: String? ->
                    if (userId != null) {
                        initializeUserProfile(userId)
                    }
                }
        }
    }

    private suspend fun initializeUserProfile(userId: String) =
        coroutineScope {
            val userProfile = accountsRepository.fetchUserInformation(UserId(userId)).onSuccess {
                Log.d(
                    "USER PROFILE",
                    "${userId} display name: ${it.displayName}"
                )
                _uiState.update {
                    it.copy(
                        currentUserId = userId,
                    )
                }
            }.onFailure {
                Log.d(
                    "USER PROFILE",
                    "Failed to get user profile: $it"
                )
            }
        }
}