package io.github.alexispurslane.bloc.ui.composables.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import io.github.alexispurslane.bloc.LoadingScreen
import io.github.alexispurslane.bloc.ui.composables.misc.UserCard
import io.github.alexispurslane.bloc.ui.composables.misc.UserRow
import io.github.alexispurslane.bloc.viewmodels.UserProfileViewModel


@Composable
fun UserProfileScreen(
    navController: NavHostController,
    userProfileViewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by userProfileViewModel.uiState.collectAsState()

    if (uiState.userProfile != null) {
        Column {
            UserCard(userProfile = uiState.userProfile!!)

            Column(
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .verticalScroll(
                        rememberScrollState()
                    )
            ) {
                Text(
                    "relationships",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Black,
                    color = Color.DarkGray,
                    style = TextStyle(
                        fontFeatureSettings = "smcp"
                    )
                )
                for (relationship in uiState.relationships) {
                    UserRow(
                        modifier = Modifier.padding(vertical = 10.dp),
                        iconSize = 40.dp,
                        userProfile = relationship.value.value,
                        relationship = relationship.key
                    )
                }
            }
        }
    } else {
        LoadingScreen()
    }
}