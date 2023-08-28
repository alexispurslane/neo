package io.github.alexispurslane.bloc.ui.models

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.data.RevoltAccountsRepository
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class ServerChannelUiState (
    val userInfo: RevoltUser? = null
)
@HiltViewModel
class ServerChannelViewModel @Inject constructor(
    private val revoltAccountRepository: RevoltAccountsRepository,
): ViewModel() {

    private val _uiState = MutableStateFlow(ServerChannelUiState())
    val uiState: StateFlow<ServerChannelUiState> = _uiState.asStateFlow()

    init {

    }
}
