package io.github.alexispurslane.bloc.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.RevoltAccountsRepository
import io.github.alexispurslane.bloc.data.network.models.RelationshipStatus
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val editing: Boolean = false,
    val currentUserId: String? = null,
    val userProfile: RevoltUser? = null,
    val relationships: Map<RelationshipStatus, RevoltUser> = mapOf()
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val revoltAccountsRepository: RevoltAccountsRepository,
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
            when (val userProfile =
                revoltAccountsRepository.fetchUserInformation(userId)) {
                is Either.Success -> {
                    Log.d(
                        "USER PROFILE",
                        "${userId} relations: ${userProfile.value.relations}"
                    )
                    _uiState.update {
                        it.copy(
                            currentUserId = userId,
                            userProfile = userProfile.value,
                            relationships = userProfile.value.relations?.map {
                                async {
                                    val userProfile =
                                        revoltAccountsRepository.fetchUserInformation(
                                            it.userId
                                        )
                                    Log.d(
                                        "USER PROFILE",
                                        "${it.status.toString()}: ${userProfile.toString()}"
                                    )
                                    if (userProfile is Either.Success) {
                                        it.status to userProfile.value
                                    } else {
                                        null
                                    }
                                }
                            }?.awaitAll()?.filterNotNull()?.associate { it!! }
                                .orEmpty()
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