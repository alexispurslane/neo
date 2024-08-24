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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import io.github.alexispurslane.bloc.R
import io.github.alexispurslane.bloc.data.RoomTree
import io.github.alexispurslane.bloc.ui.composables.misc.MatrixImage
import io.ktor.util.reflect.instanceOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import java.util.Locale

@Composable
fun ServerChannelNav(
    rooms: StateFlow<Map<RoomId, RoomTree>>,
    client: MatrixClient?,
    startingServerId: String = "",
    onNavigate: (String, String, String) -> Unit,
    lastServerChannels: Map<String, String>,
    userProfileIcon: String? = null,
) {
    val rooms by rooms.collectAsState()
    var currentServer: RoomTree.Space? by rememberSaveable { mutableStateOf(null) }
    var currentChannel: RoomTree.Channel? by rememberSaveable {
        mutableStateOf(
            null
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
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val shape =
                    if (currentServer == null) MaterialTheme.shapes.large else CircleShape
                if (userProfileIcon != null && client != null) {
                    MatrixImage(
                        modifier = Modifier
                            .aspectRatio(1.0F)
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable {
                                currentServer = null
                                onNavigate(
                                    "profile",
                                    "@me",
                                    ""
                                )
                            },
                        client = client,
                        mxcUri = userProfileIcon,
                    )
                } else {
                    OutlinedButton(
                        modifier = Modifier
                            .aspectRatio(1.0F)
                            .fillMaxWidth(),
                        shape = shape,
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            currentServer = null
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
                rooms.values.filterIsInstance<RoomTree.Space>().forEachIndexed { _, space ->
                    val shape =
                        if (space.space.roomId == currentServer?.space?.roomId) MaterialTheme.shapes.large else CircleShape
                    val elevation =
                        if (space.space.roomId == currentServer?.space?.roomId) ButtonDefaults.elevatedButtonElevation() else ButtonDefaults.buttonElevation()
                    Button(
                        modifier = Modifier
                            .aspectRatio(1.0F)
                            .fillMaxWidth(),
                        shape = shape,
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            currentServer = space
                            currentChannel = lastServerChannels[space.space.roomId.full]?.let {
                                space.children[RoomId(it)] as? RoomTree.Channel?
                            }
                        },
                        elevation = elevation
                    ) {
                        if (space.space.avatarUrl != null && client != null)
                            MatrixImage(
                                modifier = Modifier.fillMaxSize(),
                                mxcUri = space.space.avatarUrl!!,
                                client = client
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
                if (currentServer?.space?.avatarUrl != null && client != null)
                    MatrixImage(
                        modifier = Modifier.fillMaxWidth(),
                        mxcUri = currentServer?.space?.avatarUrl!!,
                        client = client
                    )
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

                    currentServer!!.children.values.groupBy { it.instanceOf(RoomTree.Channel::class) }.forEach { (isTopLevelChannel, rooms) ->
                        if (isTopLevelChannel) {
                            (rooms as List<RoomTree.Channel>).forEach { room ->
                                ChannelRow(
                                    channel = room.room,
                                    client = client,
                                    selected = currentChannel == room,
                                    onClick = {
                                        currentChannel = room
                                        onNavigate(
                                            "channel",
                                            currentServer?.space?.roomId?.full!!,
                                            room.room.roomId.full
                                        )
                                    }
                                )
                            }
                        }
                    }

                    currentServer!!.children.values.filterIsInstance<RoomTree.Space>().forEach { category ->
                        Text(
                            category.space.name?.explicitName?.lowercase(Locale.getDefault()) ?: "unknown",
                            fontSize = 15.sp,
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.Black,
                            color = Color.DarkGray,
                            style = TextStyle(fontFeatureSettings = "smcp")
                        )

                        category.children.values.filterIsInstance<RoomTree.Channel>().forEach { room ->
                            ChannelRow(
                                channel = room.room,
                                client = client,
                                selected = currentChannel == room,
                                onClick = {
                                    currentChannel = room
                                    onNavigate(
                                        "channel",
                                        currentServer?.space?.roomId?.full!!,
                                        room.room.roomId.full
                                    )
                                }
                            )
                        }
                    }
                } else {
                    rooms.values.filterIsInstance<RoomTree.Channel>().forEach { room ->
                        ChannelRow(
                            channel = room.room,
                            client = client,
                            selected = currentChannel == room,
                            onClick = {
                                currentChannel = room
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
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.channel_hashtag),
                contentDescription = "channel hashtag icon"
            )
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