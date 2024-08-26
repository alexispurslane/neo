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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.alexispurslane.bloc.R
import io.github.alexispurslane.bloc.data.RoomTree
import io.github.alexispurslane.bloc.ui.composables.misc.MatrixImage
import io.github.alexispurslane.bloc.ui.theme.EngineeringOrange
import io.github.alexispurslane.bloc.viewmodels.HomeScreenViewModel
import io.ktor.util.reflect.instanceOf
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import java.util.Locale

@Composable
fun ServerChannelNav(
    onNavigate: (String, String, String) -> Unit,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel()
) {
    val uiState by homeScreenViewModel.uiState.collectAsState()
    val rooms by homeScreenViewModel.rooms.collectAsState()
    // FIXME: why doesn't the updated last server id actually update the UI?
    val currentServerId by remember { derivedStateOf { uiState.currentServerId } }
    val currentChannelId by remember { derivedStateOf { uiState.lastServerChannels[uiState.currentServerId] } }
    val currentServer = currentServerId?.let { rooms[RoomId(it)] as? RoomTree.Space? }
    val currentChannel = currentChannelId?.let { cid -> currentServerId?.let { sid -> (rooms[RoomId(sid)] as? RoomTree.Space?)?.children?.get(RoomId(cid)) as? RoomTree.Channel? } }

    Row(
        modifier = Modifier.statusBarsPadding(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        LazyColumn(
            modifier = Modifier
                .width(64.dp)
                .padding(horizontal = 5.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val shape =
                    if (currentServerId == null) MaterialTheme.shapes.large else CircleShape
                if (uiState.userInfo?.avatarUrl != null && uiState.client != null) {
                    MatrixImage(
                        modifier = Modifier
                            .aspectRatio(1.0F)
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable {
                                homeScreenViewModel.selectServer(null)
                                onNavigate(
                                    "profile",
                                    "@me",
                                    ""
                                )
                            },
                        client = uiState.client!!,
                        mxcUri = uiState.userInfo?.avatarUrl!!,
                    )
                } else {
                    OutlinedButton(
                        modifier = Modifier
                            .aspectRatio(1.0F)
                            .fillMaxWidth(),
                        shape = shape,
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            homeScreenViewModel.selectServer(null)
                            onNavigate(
                                "profile",
                                "@me",
                                ""
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null
                        )
                    }
                }
            }

            rooms.values.filterIsInstance<RoomTree.Space>().forEachIndexed { _, space ->
                item {
                    val shape =
                        if (space.space.roomId.full == currentServerId) MaterialTheme.shapes.large else CircleShape
                    val elevation =
                        if (space.space.roomId.full == currentServerId) ButtonDefaults.elevatedButtonElevation() else ButtonDefaults.buttonElevation()
                    Button(
                        modifier = Modifier
                            .aspectRatio(1.0F)
                            .fillMaxWidth(),
                        shape = shape,
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            homeScreenViewModel.selectChannel(
                                space.space.roomId.full,
                                uiState.lastServerChannels[currentServerId]
                            )
                        },
                        elevation = elevation
                    ) {
                        if (space.space.avatarUrl != null && uiState.client != null)
                            MatrixImage(
                                modifier = Modifier.fillMaxSize(),
                                mxcUri = space.space.avatarUrl!!,
                                client = uiState.client!!
                            )
                        else
                            Text(
                                space.space.name?.explicitName?.take(2) ?: "?",
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                    }
                }
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
                if (uiState.client != null) {
                    if (currentServer?.space?.avatarUrl != null)
                        MatrixImage(
                            modifier = Modifier.fillMaxWidth(),
                            mxcUri = currentServer.space.avatarUrl!!,
                            client = uiState.client!!
                        )
                    else if (uiState.userInfo?.avatarUrl != null)
                        MatrixImage(
                            modifier = Modifier.fillMaxWidth(),
                            client = uiState.client!!,
                            mxcUri = uiState.userInfo?.avatarUrl!!
                        )
                }
                Text(
                    currentServer?.space?.name?.explicitName ?: "Direct Messages",
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
                if (currentServer != null) {
                    currentServer.children.values.groupBy { it.instanceOf(RoomTree.Channel::class) }.entries.sortedBy { it.key }.forEach { (isTopLevelChannel, rooms) ->
                        if (isTopLevelChannel) {
                            Text(
                                modifier = Modifier.padding(start = 5.dp),
                                text = "rooms",
                                fontSize = 15.sp,
                                textAlign = TextAlign.Start,
                                fontWeight = FontWeight.Black,
                                color = Color.DarkGray,
                                style = TextStyle(fontFeatureSettings = "smcp")
                            )

                            (rooms as List<RoomTree.Channel>).sortedByDescending { it.room.lastRelevantEventTimestamp }.forEach { room ->
                                ChannelRow(
                                    channel = room.room,
                                    client = uiState.client,
                                    selected = currentChannel == room,
                                    onClick = {
                                        homeScreenViewModel.selectChannel(
                                            currentServer.space.roomId.full,
                                            room.room.roomId.full
                                        )
                                        onNavigate(
                                            "channel",
                                            currentServer.space.roomId.full,
                                            room.room.roomId.full
                                        )
                                    }
                                )
                            }
                        } else {
                            (rooms as List<RoomTree.Space>).forEach { category ->
                                Text(
                                    modifier = Modifier.padding(start = 5.dp),
                                    text = category.space.name?.explicitName?.lowercase(Locale.getDefault()) ?: "unknown",
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Start,
                                    fontWeight = FontWeight.Black,
                                    color = Color.DarkGray,
                                    style = TextStyle(fontFeatureSettings = "smcp")
                                )

                                category.children.values.filterIsInstance<RoomTree.Channel>().forEach { room ->
                                    ChannelRow(
                                        channel = room.room,
                                        client = uiState.client,
                                        selected = currentChannel == room,
                                        onClick = {
                                            homeScreenViewModel.selectChannel(
                                                currentServer.space.roomId.full,
                                                room.room.roomId.full
                                            )
                                            onNavigate(
                                                "channel",
                                                currentServer.space.roomId.full,
                                                room.room.roomId.full
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    rooms.values.filterIsInstance<RoomTree.Channel>().forEach { room ->
                        ChannelRow(
                            channel = room.room,
                            client = uiState.client,
                            selected = currentChannel == room,
                            onClick = {
                                homeScreenViewModel.selectChannel(
                                    "@me",
                                    room.room.roomId.full
                                )
                                onNavigate(
                                    "channel",
                                    "@me",
                                    room.room.roomId.full
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelRow(
    channel: Room,
    selected: Boolean,
    client: MatrixClient?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .height(38.dp)
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
        val textColor =
            if (selected) MaterialTheme.colorScheme.onSecondaryContainer else Color.Gray
        Box(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .size(20.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.channel_hashtag),
                contentDescription = "channel hashtag icon",
                tint = textColor
            )
        }
        Text(
            channel.name?.explicitName ?: "?",
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            fontSize = 16.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // FIXME: This doesn't work
        if (channel.unreadMessageCount > 0) {
            Box(
                modifier = Modifier.background(EngineeringOrange).clip(CircleShape).width(10.dp),
            ) {
                Text(
                    text = channel.unreadMessageCount.toString(),
                    color = Color.Black,
                    fontSize = 11.sp
                )
            }
        }
    }
}