package io.github.alexispurslane.bloc

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import io.github.alexispurslane.bloc.data.RevoltAccountsRepository
import io.github.alexispurslane.bloc.ui.composables.screens.LoginScreen
import io.github.alexispurslane.bloc.ui.composables.screens.HomeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun MainScreen(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val mainState by mainViewModel.uiState.collectAsState()

    when (mainState.state) {
        MainUiScreenState.LOADING -> LoadingScreen()
        MainUiScreenState.LOGGED_OUT -> LoginScreen(setLoggedIn = mainViewModel::setLoggedIn)
        MainUiScreenState.LOGGED_IN ->
            HomeScreen(setLoggedIn = mainViewModel::setLoggedIn)
    }
}

@Preview
@Composable
fun LoadingScreen() {
    var showLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(450)
        showLoading = true
    }

    if (showLoading) {
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
}

enum class MainUiScreenState {
    LOADING,
    LOGGED_OUT,
    LOGGED_IN
}

data class MainUiState(
    val state: MainUiScreenState = MainUiScreenState.LOADING
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val revoltAccountsRepository: RevoltAccountsRepository,
): ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun setLoggedIn(b: Boolean) {
        _uiState.update {
            it.copy(
                state = if (b) MainUiScreenState.LOGGED_IN else MainUiScreenState.LOGGED_OUT
            )
        }
    }

    init {
        viewModelScope.launch {
            revoltAccountsRepository.userSessionFlow.collect { userSession ->
                setLoggedIn(userSession.sessionToken != null)
            }
        }
    }
}
