package io.github.alexispurslane.bloc

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.alexispurslane.bloc.data.AccountsRepository
import io.github.alexispurslane.bloc.data.RoomsRepository
import io.github.alexispurslane.bloc.ui.composables.screens.HomeScreen
import io.github.alexispurslane.bloc.ui.composables.screens.LoginScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import javax.inject.Inject

@Composable
fun MainScreen(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val mainState by mainViewModel.uiState.collectAsState()

    LaunchedEffect(true) {
        mainViewModel.showLoginScreenAfterDelay()
    }

    when (mainState.state) {
        MainUiScreenState.LOADING -> LoadingScreen()
        MainUiScreenState.LOGGED_OUT -> LoginScreen()
        MainUiScreenState.LOGGED_IN ->
            HomeScreen()
    }
}

@Preview
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 50.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Loading...",
                fontWeight = FontWeight.Black,
                fontSize = 30.sp,
                textAlign = TextAlign.Center
            )
            Text(
                Constants.LOADING_SCREEN_REMARKS.random(),
                fontWeight = FontWeight.Black,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.LightGray,
            )
        }
    }
}

enum class MainUiScreenState {
    LOADING,
    LOGGED_OUT,
    LOGGED_IN
}

data class MainUiState(
    var state: MainUiScreenState = MainUiScreenState.LOADING
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val roomsRepository: RoomsRepository,
): ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountsRepository.matrixClientFlow.filterNotNull().first()
            accountsRepository.matrixClient?.loginState?.first()?.let { loginState ->
                if (loginState == MatrixClient.LoginState.LOGGED_IN) {
                    _uiState.update {
                        it.copy(
                            state = MainUiScreenState.LOGGED_IN
                        )
                    }
                }
            }
        }
    }

    fun showLoginScreenAfterDelay() {
        viewModelScope.launch {
            delay(500)
            if (uiState.value.state != MainUiScreenState.LOGGED_IN) {
                Log.d("Main Screen", "Still not logged in, assuming new login needed")
                _uiState.update {
                    it.copy(
                        state = MainUiScreenState.LOGGED_OUT
                    )
                }
            }
        }
    }
}
