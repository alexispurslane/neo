package io.github.alexispurslane.bloc.ui.composables.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.github.alexispurslane.bloc.LoadingScreen
import io.github.alexispurslane.bloc.ui.composables.misc.MessagesView
import io.github.alexispurslane.bloc.ui.composables.navigation.ChannelTopBar
import io.github.alexispurslane.bloc.ui.composables.navigation.MessageBar
import io.github.alexispurslane.bloc.viewmodels.ChannelViewModel

@Composable
fun ChannelViewScreen(
    navController: NavController,
    channelViewModel: ChannelViewModel = hiltViewModel()
) {
    val uiState by channelViewModel.uiState.collectAsState()

    if (uiState.channelInfo == null) {
        LoadingScreen()
    } else {
        Scaffold(
            topBar = {
                ChannelTopBar(uiState.channelInfo!!)
            },
            content = {
                if (uiState.error != null) {
                    val error = uiState.error!!.split(':')
                    ErrorScreen(title = error[0], message = error[1])
                } else {
                    MessagesView(
                        modifier = Modifier.padding(it),
                        uiState,
                        uiState.channelInfo!!,
                        onProfileClick = { userId ->
                            navController.navigate("profile/$userId")
                        }
                    )
                }
            },
            bottomBar = {
                MessageBar(
                    uiState.channelInfo?.name?.explicitName ?: "?",
                    uiState.draftMessage,
                    channelViewModel::updateMessage,
                    channelViewModel::sendMessage
                )
            }
        )
    }
    if (uiState.isSendError) {
        ErrorDialog(
            channelViewModel::onDialogDismiss,
            uiState.sendErrorTitle,
            uiState.sendErrorText
        )
    } else if (uiState.error != null) {
        ErrorDialog(
            channelViewModel::onDialogDismiss,
            "Uh oh!",
            uiState.error!!
        )
    }
}