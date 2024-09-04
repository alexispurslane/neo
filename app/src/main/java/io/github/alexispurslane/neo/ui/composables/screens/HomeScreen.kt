package io.github.alexispurslane.neo.ui.composables.screens

import android.util.Log
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import io.github.alexispurslane.neo.LoadingScreen
import io.github.alexispurslane.neo.ui.composables.navigation.ServerChannelNav
import io.github.alexispurslane.neo.viewmodels.HomeScreenViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val uiState by homeScreenViewModel.uiState.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val layoutDir = if (uiState.isNavDrawerOrientationRtl) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        DismissibleNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                DismissibleDrawerSheet(
                ) {
                    ServerChannelNav(
                        onNavigate = { type, serverId, channelId ->
                            when (type) {
                                "profile" -> navController.navigate("profile/$serverId")
                                "channel" -> {
                                    navController.navigate("channel/$channelId")
                                }

                                else -> {}
                            }

                            scope.launch {
                                drawerState.close()
                            }
                        },
                    )
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier.imePadding(),
                ) {
                    NavHost(navController, startDestination = "loading") {
                        composable("loading") {
                            val loadedUserInfo by remember { derivedStateOf { uiState.userInfo != null } }
                            LaunchedEffect(loadedUserInfo) {
                                Log.d("Home Screen Nav", "Loaded user info. ${uiState.currentServerId}")
                                if (uiState.currentServerId != null) {
                                    navController.navigate(
                                        "channel/${uiState.lastServerChannels[uiState.currentServerId]}"
                                    )
                                } else if (loadedUserInfo) {
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
                                uriPattern = "neo://profile/{userId}"
                            })
                        ) {
                            UserProfileScreen(navController)
                        }
                        composable(
                            "channel/{channelId}",
                            deepLinks = listOf(navDeepLink {
                                uriPattern = "neo://channel/{channelId}"
                            })
                        ) {
                            // channelId argument automatically passed to
                            // ServerChannelViewModel by SavedStateHandle!
                            ChannelViewScreen(navController)
                        }
                        composable("channel/") {
                            ChannelViewScreen(navController)
                        }
                    }
                }
            }
        }
    }
}