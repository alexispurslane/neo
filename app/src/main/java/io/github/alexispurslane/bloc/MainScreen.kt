package io.github.alexispurslane.bloc

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.alexispurslane.bloc.ui.LoginScreen

@Composable
fun MainScreen() {
    val (loggedIn, setLoggedIn) = remember { mutableStateOf(false) }

    if (!loggedIn) {
        LoginScreen(
            setLoggedIn = setLoggedIn
        )
    } else {
        TODO("Home screen not implemented yet")
    }
}