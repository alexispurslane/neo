package io.github.alexispurslane.bloc.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.RevoltAccountsRepository
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.models.RelationshipStatus
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val editing: Boolean = false,
    val userProfile: RevoltUser? = null,
    val relationships: Map<RelationshipStatus, RevoltUser> = mapOf()
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val revoltAccountsRepository: RevoltAccountsRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            revoltAccountsRepository.userSessionFlow.collect { userSession ->
                if (userSession.instanceApiUrl != null) {
                    RevoltApiModule.setBaseUrl(userSession.instanceApiUrl)
                    when (val userProfile = revoltAccountsRepository.fetchUserInformation()) {
                        is Either.Success -> {
                            Log.d(
                                "USER PROFILE",
                                "Successful fetch user info: ${userProfile.value.toString()}"
                            )
                            _uiState.update {
                                it.copy(
                                    userProfile = userProfile.value,
                                    relationships = userProfile.value.relations?.mapNotNull {
                                        val userProfile =
                                            revoltAccountsRepository.fetchUserInformation(
                                                it.userId
                                            )
                                        Log.d("USER PROFILE", "${it.status.toString()}: ${userProfile.toString()}")
                                        if (userProfile is Either.Success) {
                                            it.status to userProfile.value
                                        } else {
                                            null
                                        }
                                    }?.associate { it!! }.orEmpty()
                                )
                            }
                        }

                        is Either.Error -> {
                            Log.d(
                                "USER PROFILE",
                                "Failed to get user profile: ${userProfile.value}"
                            )
                        }
                    }
                }
            }
        }
    }
}
