package io.github.alexispurslane.bloc.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import io.github.alexispurslane.bloc.LoadingScreen
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.models.Presence
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import io.github.alexispurslane.bloc.ui.models.UserProfileViewModel


@Composable
fun UserProfileScreen(
    navController: NavHostController,
    userProfileViewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by userProfileViewModel.uiState.collectAsState()

    if (uiState.userProfile != null) {
        Column {
            UserCard(userProfile = uiState.userProfile!!)
            
            for (relationship in uiState.relationships) {
                UserCard(userProfile = relationship.value)
            }
        }
    } else {
        LoadingScreen()
    }
}

@Composable
fun UserCard(
    modifier: Modifier = Modifier,
    userProfile: RevoltUser,
) {
    val backgroundImage = userProfile.profile?.background
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth(1f)
            .clip(RoundedCornerShape(1))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box {
                if (backgroundImage != null) {
                    AsyncImage(
                        modifier = Modifier
                            .fillMaxWidth(),
                        model = RevoltApiModule.getResourceUrl(backgroundImage),
                        contentDescription = "User Background",
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(Color(0x66000000), BlendMode.Darken)
                    )
                }
                UserRow(userProfile = userProfile)
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("INFORMATION", fontSize = 15.sp, textAlign = TextAlign.Start, fontWeight = FontWeight.Black, color = Color.DarkGray, style = TextStyle(
                    fontFeatureSettings = "smcp"
                ))
                Text(userProfile.profile?.content ?: "", fontSize = 15.sp, textAlign = TextAlign.Start)
            }
        }
    }
}

@Composable
fun UserRow(
    modifier: Modifier = Modifier,
    userProfile: RevoltUser
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(30.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (userProfile.avatar != null) {
            UserAvatar(
                size = 64.dp,
                userProfile = userProfile
            )
        }
        Column {
            Text("${userProfile.displayName}",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Start)
            Text("@${userProfile.userName}#${userProfile.discriminator}",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start)
            Text(userProfile.status?.customStatus ?: "",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                color = Color.LightGray)
        }
    }
}

@Composable
fun UserAvatar(
    modifier: Modifier = Modifier,
    size: Dp,
    userProfile: RevoltUser,
) {
    Box {
        if (userProfile.avatar != null) {
            AsyncImage(
                modifier = modifier
                    .size(size)
                    .aspectRatio(1f)
                    .clip(CircleShape),
                model = RevoltApiModule.getResourceUrl(userProfile.avatar),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = modifier
                    .size(size)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "No Avatar User Icon"
                )
            }
        }

        val presenceColor =
            when (userProfile.status?.presence ?: Presence.ONLINE) {
                Presence.ONLINE -> Color(0xFF3ABF7E)
                Presence.IDLE -> Color.Yellow
                Presence.BUSY -> Color.Red
                Presence.FOCUS -> Color.Magenta
                Presence.Invisible -> Color.Transparent
            }
        val color =
            if (userProfile.online == true || userProfile.online == null) {
                presenceColor
            } else {
                Color.Gray
            }

        val statusSize = (size / 3)
        val statusOffset = (size / 2 - statusSize / 2 - 1.dp)
        Box(
            modifier = Modifier
                .offset(x = statusOffset, y = statusOffset)
                .size(statusSize)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(color)
                .align(Alignment.Center)
        )
    }
}
