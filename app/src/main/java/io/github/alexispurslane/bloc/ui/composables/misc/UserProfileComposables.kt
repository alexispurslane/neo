package io.github.alexispurslane.bloc.ui.composables.misc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.alexispurslane.bloc.data.local.RevoltAutumnModule
import io.github.alexispurslane.bloc.data.network.models.Masquerade
import io.github.alexispurslane.bloc.data.network.models.Presence
import io.github.alexispurslane.bloc.data.network.models.RelationshipStatus
import io.github.alexispurslane.bloc.data.network.models.RevoltServerMember
import io.github.alexispurslane.bloc.data.network.models.RevoltUser


@Composable
fun UserCard(
    modifier: Modifier = Modifier,
    userProfile: State<RevoltUser>,
) {
    val backgroundImage = userProfile.value.profile?.background
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
                        model = RevoltAutumnModule.getResourceUrl(
                            LocalContext.current,
                            backgroundImage
                        ),
                        contentDescription = "User Background",
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.tint(
                            Color(0x77000000),
                            BlendMode.Darken
                        )
                    )
                }
                UserRow(
                    modifier = Modifier.padding(30.dp),
                    userProfile = userProfile.value
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 30.dp)
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "information",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Black,
                    color = Color.DarkGray,
                    style = TextStyle(
                        fontFeatureSettings = "smcp"
                    )
                )

                CustomizableMarkdownText(
                    content = userProfile.value.profile?.content ?: "",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
fun UserRow(
    modifier: Modifier = Modifier,
    iconSize: Dp = 64.dp,
    userProfile: RevoltUser,
    relationship: RelationshipStatus? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (userProfile.avatar != null) {
            UserAvatar(
                size = iconSize,
                userProfile = userProfile
            )
        }
        Column {
            if (userProfile.displayName != null) {
                Text(
                    "${userProfile.displayName}",
                    fontSize = (iconSize.value / 2 - 4).sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Start
                )
                Text(
                    "@${userProfile.userName}#${userProfile.discriminator}",
                    fontSize = (iconSize.value / 4 - 4).sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Start,
                    color = Color.LightGray
                )
            } else {
                Text(
                    "@${userProfile.userName}#${userProfile.discriminator}",
                    fontSize = (iconSize.value / 2 - 4).sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Start
                )
            }
            if (relationship != null) {
                val color = when (relationship) {
                    RelationshipStatus.FRIEND -> {
                        Color.White
                    }

                    RelationshipStatus.BLOCKED -> {
                        Color(0xFFE32912)
                    }

                    RelationshipStatus.BLOCKED_OTHER -> {
                        Color(0xFFE32912)
                    }

                    RelationshipStatus.INCOMING -> {
                        MaterialTheme.colorScheme.secondary
                    }

                    RelationshipStatus.OUTGOING -> {
                        MaterialTheme.colorScheme.secondary
                    }

                    RelationshipStatus.NONE -> {
                        Color.DarkGray
                    }

                    RelationshipStatus.USER -> {
                        Color.LightGray
                    }
                }
                Text(
                    relationship.status,
                    fontSize = 12.sp,
                    color = color
                )
            }
            Text(
                userProfile.status?.customStatus ?: "",
                fontSize = (iconSize.value / 4 - 4).sp,
                style = TextStyle(lineHeight = 15.sp),
                textAlign = TextAlign.Start,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun UserAvatar(
    modifier: Modifier = Modifier,
    size: Dp,
    userProfile: RevoltUser,
    member: RevoltServerMember? = null,
    masquerade: Masquerade? = null,
    onClick: (String) -> Unit = {}
) {
    Box {
        if (userProfile.avatar != null) {
            AsyncImage(
                modifier = modifier
                    .size(size)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .clickable { onClick(userProfile.userId) },
                model = masquerade?.avatarUrl
                    ?: RevoltAutumnModule.getResourceUrl(
                        LocalContext.current,
                        member?.avatar ?: userProfile.avatar
                    ),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = modifier
                    .size(size)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { onClick(userProfile.userId) },
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
                Presence.FOCUS -> Color.Blue
                Presence.BUSY -> Color(0xFFE32912)
                Presence.Invisible -> Color.LightGray
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