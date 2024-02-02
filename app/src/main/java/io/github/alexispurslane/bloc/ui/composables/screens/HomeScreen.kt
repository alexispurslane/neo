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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.navigation.navDeepLink
import io.github.alexispurslane.bloc.LoadingScreen
import io.github.alexispurslane.bloc.ui.composables.misc.ScrollableThreeDrawerScaffold
import io.github.alexispurslane.bloc.ui.composables.navigation.ServerChannelNav
import io.github.alexispurslane.bloc.viewmodels.HomeScreenViewModel

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
                "Welcome, ${uiState.userInfo?.value?.userName}!",
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

    var currentChannelId by rememberSaveable { mutableStateOf<String?>(null) }

    ScrollableThreeDrawerScaffold(
        left = { reset ->
            ServerChannelNav(
                servers = uiState.servers,
                channels = uiState.channels,
                startingServerId = uiState.lastServer ?: "",
                onNavigate = { type, serverId, channelId ->
                    when (type) {
                        "settings" -> navController.navigate("settings")
                        "profile" -> navController.navigate("profile/${uiState.userInfo?.value?.userId ?: "@me"}")
                        "channel" -> {
                            homeScreenViewModel.saveLast(
                                serverId,
                                channelId
                            )
                            navController.navigate("channel/${channelId}")
                        }

                        else -> {}
                    }
                    reset()
                },
                lastServerChannels = uiState.lastServerChannels,
                userProfileIcon = uiState.userInfo?.value?.avatar
            )
        },
        middle = {
            NavHost(navController, startDestination = "loading") {
                composable("loading") {
                    val loadedUserInfo by remember { derivedStateOf { uiState.userInfo != null && uiState.servers.isNotEmpty() } }
                    LaunchedEffect(loadedUserInfo) {
                        if (loadedUserInfo) {
                            navController.navigate(
                                if (uiState.lastServer != null && uiState.lastChannel != null)
                                    "channel/${uiState.lastChannel}"
                                else
                                    "welcome"
                            )
                        }
                    }
                    LoadingScreen()
                }
                composable("welcome") { WelcomeScreen(setLoggedIn = setLoggedIn) }
                composable(
                    "profile/{userId}",
                    deepLinks = listOf(navDeepLink {
                        uriPattern = "bloc://profile/{userId}"
                    })
                ) {
                    UserProfileScreen(navController)
                }
                composable(
                    "channel/{channelId}",
                    deepLinks = listOf(navDeepLink {
                        uriPattern = "bloc://channel/{channelId}"
                    })
                ) {
                    currentChannelId = it.arguments?.getString("channelId")
                    // channelId argument automatically passed to
                    // ServerChannelViewModel by SavedStateHandle!
                    ChannelViewScreen(navController)
                }
                composable("settings") { SettingsScreen() }
            }
        },
        right = {
            ChannelViewPullout(navController, currentChannelId)
        },
    )
}