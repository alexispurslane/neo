package io.github.alexispurslane.bloc.ui.composables.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.github.alexispurslane.bloc.R
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId

@Composable
fun ServerChannelNav(
    servers: Map<RoomId, Room>,
    channels: Map<RoomId, Map<RoomId, Room>>,
    startingServerId: String = "",
    onNavigate: (String, String, String) -> Unit,
    lastServerChannels: Map<String, String>,
    userProfileIcon: String? = null
) {
    var currentServerId by rememberSaveable { mutableStateOf(startingServerId) }
    var currentChannelId by rememberSaveable {
        mutableStateOf(
            lastServerChannels[startingServerId] ?: ""
        )
    }

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .width(64.dp)
                .padding(horizontal = 5.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val shape =
                    if (currentServerId == "@dms") MaterialTheme.shapes.large else CircleShape
                if (userProfileIcon != null) {
                    AsyncImage(
                        modifier = Modifier
                            .aspectRatio(1.0F)
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable {
                                currentServerId = "@dms"
                                currentChannelId =
                                    lastServerChannels[currentServerId] ?: ""
                                onNavigate(
                                    "profile",
                                    currentServerId,
                                    currentChannelId
                                )
                            },
                        model = userProfileIcon,
                        contentDescription = "User Avatar"
                    )
                } else {
                    OutlinedButton(
                        modifier = Modifier
                            .aspectRatio(1.0F)
                            .fillMaxWidth(),
                        shape = shape,
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            currentServerId = "@dms"
                            currentChannelId =
                                lastServerChannels[currentServerId] ?: ""
                            onNavigate(
                                "profile",
                                currentServerId,
                                currentChannelId
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null
                        )
                    }
                }
                servers.values.forEachIndexed { _, server ->
                    val shape =
                        if (server.roomId.full == currentServerId) MaterialTheme.shapes.large else CircleShape
                    val elevation =
                        if (server.roomId.full == currentServerId) ButtonDefaults.elevatedButtonElevation() else ButtonDefaults.buttonElevation()
                    Button(
                        modifier = Modifier
                            .aspectRatio(1.0F)
                            .fillMaxWidth(),
                        shape = shape,
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            currentServerId = server.roomId.full
                            currentChannelId =
                                lastServerChannels[currentServerId] ?: ""
                        },
                        elevation = elevation
                    ) {
                        if (server.avatarUrl != null)
                            AsyncImage(
                                modifier = Modifier.fillMaxSize(),
                                model = server.avatarUrl!!,
                                contentDescription = "Server Icon"
                            )
                        else
                            Text(
                                server.name?.explicitName?.take(2) ?: "?",
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                    }
                }
            }
            IconButton(
                modifier = Modifier
                    .aspectRatio(1.0F)
                    .fillMaxWidth(),
                onClick = {
                    onNavigate("settings", currentServerId, currentChannelId)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(160.dp)
                    .background(Color(0x22000000)),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    servers[RoomId(currentServerId)]?.name?.explicitName ?: "Direct Messages",
                    modifier = Modifier.padding(start = 10.dp),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Start,
                    color = Color.White
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                channels[RoomId(currentServerId)]?.forEach { room ->
                    ChannelRow(
                        channel = room.value,
                        selected = currentChannelId == room.key.full,
                        onClick = {
                            currentChannelId = room.key.full
                            onNavigate(
                                "channel",
                                currentServerId,
                                room.key.full
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelRow(
    channel: Room,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .height(40.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                if (selected) Color(
                    0x55000000
                ) else Color.Transparent
            )
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .size(24.dp)
        ) {
                if (channel.avatarUrl != null) {
                    AsyncImage(
                        model = channel.avatarUrl,
                        contentDescription = "Channel Icon",
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.channel_hashtag),
                        contentDescription = "channel hashtag icon"
                    )
                }
        }
        val textColor =
            if (selected) MaterialTheme.colorScheme.onSecondaryContainer else Color.Gray
        Text(
            channel.name?.explicitName ?: "?",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            fontSize = 20.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}