package io.github.alexispurslane.bloc.viewmodels

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.MainApplication
import io.github.alexispurslane.service.Actions
import io.github.alexispurslane.service.ServiceState
import io.github.alexispurslane.service.WebSocketNotificationService
import io.github.alexispurslane.service.getServiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
    val darkTheme: Boolean = false,
    val serviceOn: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: MainApplication,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        updateServiceState()
    }

    fun updateServiceState() {
        _uiState.update {
            it.copy(serviceOn = getServiceState(application.applicationContext) == ServiceState.STARTED)
        }
    }

    fun toggleDarkTheme(value: Boolean) {
        Log.d("SETTINGS VIEW", "Toggle dark theme")
        _uiState.update {
            it.copy(darkTheme = value)
        }
    }

    fun toggleNotificationsService(value: Boolean) {
        if (!value && getServiceState(application.applicationContext) == ServiceState.STOPPED) return
        Intent(
            application.applicationContext,
            WebSocketNotificationService::class.java
        ).apply {
            action = if (value) Actions.START.name else Actions.STOP.name
            application.startForegroundService(this)
        }
        _uiState.update {
            it.copy(serviceOn = value)
        }
    }
}