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
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import io.github.alexispurslane.bloc.R
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.data.network.models.RevoltServer
import java.util.Locale

@Composable
fun ServerChannelNav(
    currentServerId: MutableState<String>,
    currentChannelId: MutableState<String>,
    navController: NavHostController,
    servers: Map<String, RevoltServer>,
    channels: Map<String, RevoltChannel>,
    onNavigate: () -> Unit
) {
    var currentServerId by currentServerId
    var currentChannelId by currentChannelId
    val configuration = LocalConfiguration.current

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
                OutlinedButton(
                    modifier = Modifier
                        .aspectRatio(1.0F)
                        .fillMaxWidth(),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        if (navController.currentDestination?.navigatorName != "profile") {
                            navController.navigate("profile")
                        }
                        onNavigate()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null
                    )
                }
                OutlinedButton(
                    modifier = Modifier
                        .aspectRatio(1.0F)
                        .fillMaxWidth(),
                    shape = if (currentServerId == "@dms") MaterialTheme.shapes.large else CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        currentServerId = "@dms"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null
                    )
                }
                servers.values.forEachIndexed { index, server ->
                    val shape =
                        if (server.serverId == currentServerId) MaterialTheme.shapes.large else CircleShape
                    val elevation =
                        if (server.serverId == currentServerId) ButtonDefaults.elevatedButtonElevation() else ButtonDefaults.buttonElevation()
                    Button(
                        modifier = Modifier
                            .aspectRatio(1.0F)
                            .fillMaxWidth(),
                        shape = shape,
                        contentPadding = PaddingValues(0.dp),
                        onClick = {
                            currentServerId = server.serverId
                        },
                        elevation = elevation
                    ) {
                        if (server.icon != null)
                            AsyncImage(
                                modifier = Modifier.fillMaxSize(),
                                model = RevoltApiModule.getResourceUrl(server.icon!!),
                                contentDescription = "Server Icon"
                            )
                        else
                            Text(
                                server.name.take(2),
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
                    if (navController.currentDestination?.navigatorName != "settings") {
                        navController.navigate("settings")
                        onNavigate()
                    }
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
                if (servers.get(currentServerId)?.banner != null)
                    AsyncImage(
                        model = RevoltApiModule.getResourceUrl(servers[currentServerId]!!.banner!!),
                        contentDescription = "Server Banner",
                        contentScale = ContentScale.FillHeight
                    )
                Text(
                    servers.get(currentServerId)?.name ?: "Direct Messages",
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
                servers.get(currentServerId)?.channelsIds?.forEach { channelId ->
                    val channel = channels[channelId]
                    val notInCategory =
                        servers.get(currentServerId)?.categories?.find {
                            it.channelIds.contains(channelId)
                        } == null
                    if (channel != null && channel is RevoltChannel.TextChannel && notInCategory) {
                        ChannelRow(
                            channel = channel,
                            selected = currentChannelId == channelId
                        ) {
                            currentChannelId = channelId
                        }
                    }
                }
                servers.get(currentServerId)?.categories?.forEach { category ->
                    Text(
                        category.title.lowercase(Locale.getDefault()),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Black,
                        color = Color.DarkGray,
                        style = TextStyle(fontFeatureSettings = "smcp")
                    )
                    category.channelIds.forEachIndexed { index, channelId ->
                        val channel = channels[channelId]
                        if (channel != null)
                            ChannelRow(
                                channel = channel,
                                selected = channelId == currentChannelId,
                                onClick = {
                                    currentChannelId = channelId
                                    navController.navigate("channel/${currentChannelId}")
                                    onNavigate()
                                }
                            )
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelRow(channel: RevoltChannel, selected: Boolean, onClick: () -> Unit) {
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
        if (channel is RevoltChannel.TextChannel)
            Text(channel.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start, fontSize = 20.sp, color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis)
    }
}