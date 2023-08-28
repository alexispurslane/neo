package io.github.alexispurslane.bloc.ui.composables.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.alexispurslane.bloc.ui.composables.misc.ScrollableThreeDrawerScaffold
import io.github.alexispurslane.bloc.ui.composables.navigation.ServerChannelNav
import io.github.alexispurslane.bloc.ui.models.HomeScreenViewModel

@Composable
fun WelcomeScreen(
    setLoggedIn: (Boolean) -> Unit,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel()
) {
    val uiState by homeScreenViewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .padding(horizontal = 50.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Welcome, ${uiState.userInfo?.userName}!",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )

            OutlinedButton(
                modifier = Modifier.height(50.dp),
                onClick = {
                    homeScreenViewModel.logout()
                    setLoggedIn(false)
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Log Out")
            }
        }
    }
}

@Composable
fun HomeScreen(
    setLoggedIn: (Boolean) -> Unit,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val uiState by homeScreenViewModel.uiState.collectAsState()

    ScrollableThreeDrawerScaffold(
        left = {
            ServerChannelNav(navController)
        },
        middle = {
            NavHost(navController, startDestination = "welcome") {
                composable("welcome") { WelcomeScreen(setLoggedIn = setLoggedIn) }
                composable("profile") { UserProfileScreen(navController) }
            }
        },
        right = {
        }
    )
}
