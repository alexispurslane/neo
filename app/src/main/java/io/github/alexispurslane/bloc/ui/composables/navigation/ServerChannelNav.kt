package io.github.alexispurslane.bloc.ui.composables.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import io.github.alexispurslane.bloc.R
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.data.network.models.RevoltServer

@Composable
fun ServerChannelNav(navController: NavHostController, servers: List<RevoltServer>) {
    val configuration = LocalConfiguration.current

    var currentServer by remember { mutableStateOf(-1) }
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .width(64.dp)
                .padding(horizontal = 5.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                modifier = Modifier
                    .aspectRatio(1.0F)
                    .fillMaxWidth(),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                onClick = { navController.navigate("profile") }
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
                shape = if (currentServer == -1) MaterialTheme.shapes.large else CircleShape,
                contentPadding = PaddingValues(0.dp),
                onClick = {
                    currentServer = -1
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null
                )
            }
            servers.forEachIndexed { index, server ->
                val shape = if (index == currentServer) MaterialTheme.shapes.large else CircleShape
                val elevation = if (index == currentServer) ButtonDefaults.elevatedButtonElevation() else ButtonDefaults.buttonElevation()
                Button(
                    modifier = Modifier
                        .aspectRatio(1.0F)
                        .fillMaxWidth(),
                    shape = shape,
                    contentPadding = PaddingValues(0.dp),
                    onClick = {
                        currentServer = index
                    },
                    elevation = elevation
                ) {
                    if (server.icon != null)
                        AsyncImage(model = RevoltApiModule.getResourceUrl(server.icon!!), contentDescription = "Server Icon")
                    else
                        Text(server.name.take(2), fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
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
                    .requiredHeight(150.dp)
                    .background(Color(0x22000000))
                    .padding(10.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                if (servers.getOrNull(currentServer)?.banner != null)
                    AsyncImage(model = RevoltApiModule.getResourceUrl(servers[currentServer].banner!!), contentDescription = "Server Banner")
                Text(servers.getOrNull(currentServer)?.name ?: "@dms", fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Start)
            }
            for (channel in emptyList<RevoltChannel>()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .height(40.dp)
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(
                            if (false) Color(
                                0x55000000
                            ) else Color.Transparent
                        )
                        .clickable {
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
                    val textColor = if (false) Color.LightGray else Color.Gray
                    Text("unknown-channel", fontWeight = FontWeight.Bold, textAlign = TextAlign.Start, fontSize = 20.sp, color = textColor)
                }
            }
        }
    }
}