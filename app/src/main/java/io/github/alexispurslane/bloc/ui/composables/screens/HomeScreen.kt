package io.github.alexispurslane.bloc.ui.composables.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import io.github.alexispurslane.bloc.LoadingScreen
import io.github.alexispurslane.bloc.ui.composables.misc.ScrollableThreeDrawerScaffold
import io.github.alexispurslane.bloc.ui.composables.navigation.ServerChannelNav
import io.github.alexispurslane.bloc.viewmodels.HomeScreenViewModel

@Composable
fun HomeScreen(
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val uiState by homeScreenViewModel.uiState.collectAsStateWithLifecycle()

    ScrollableThreeDrawerScaffold(
        left = { reset ->
            ServerChannelNav(
                rooms = homeScreenViewModel.rooms,
                client = uiState.client,
                startingServerId = uiState.lastServer ?: "",
                onNavigate = { type, serverId, channelId ->
                    when (type) {
                        "profile" -> navController.navigate("profile/$serverId")
                        "channel" -> {
                            homeScreenViewModel.saveLast(
                                serverId,
                                channelId
                            )
                            navController.navigate("channel/$serverId/$channelId")
                        }

                        else -> {}
                    }
                    reset()
                },
                lastServerChannels = uiState.lastServerChannels,
                userProfileIcon = uiState.userInfo?.avatarUrl,
            )
        },
        middle = {
            NavHost(navController, startDestination = "loading") {
                composable("loading") {
                    val loadedUserInfo by remember { derivedStateOf { uiState.userInfo != null && homeScreenViewModel.rooms.value.isNotEmpty() } }
                    LaunchedEffect(loadedUserInfo) {
                        if (loadedUserInfo) {
                            navController.navigate(
                                "profile/@me"
                            )
                        }
                    }
                    LoadingScreen()
                }
                composable(
                    "profile/{userId}",
                    deepLinks = listOf(navDeepLink {
                        uriPattern = "bloc://profile/{userId}"
                    })
                ) {
                    UserProfileScreen(navController)
                }
                composable(
                    "channel/{serverId}/{channelId}",
                    deepLinks = listOf(navDeepLink {
                        uriPattern = "bloc://channel/{serverId}/{channelId}"
                    })
                ) {
                    // channelId argument automatically passed to
                    // ServerChannelViewModel by SavedStateHandle!
                    ChannelViewScreen(navController)
                }
            }
        },
        right = {
        },
    )
}