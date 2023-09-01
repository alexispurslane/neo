package io.github.alexispurslane.bloc.ui.composables.misc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material3.Material3RichText
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.data.network.models.RevoltFileMetadata
import io.github.alexispurslane.bloc.data.network.models.RevoltMessage
import io.github.alexispurslane.bloc.data.network.models.RevoltServerMember
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import io.github.alexispurslane.bloc.data.network.models.Role
import io.github.alexispurslane.bloc.ui.composables.screens.UserAvatar

@Composable
fun BeginningMessage(
    modifier: Modifier = Modifier,
    channelInfo: RevoltChannel.TextChannel
) {
    Box(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .padding(bottom = 5.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Text(
                "#${channelInfo.name}",
                fontWeight = FontWeight.Black,
                fontSize = 40.sp,
            )
            Text(
                "This is the beginning of your legendary conversation!",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
fun Message(
    modifier: Modifier = Modifier,
    message: RevoltMessage,
    user: RevoltUser,
    member: RevoltServerMember,
    role: Role?,
    prevMessage: RevoltMessage? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                10.dp,
                Alignment.Start
            ),
            verticalAlignment = Alignment.Top
        ) {
            if (prevMessage == null || prevMessage?.authorId != message.authorId) {
                UserAvatar(
                    size = 40.dp,
                    userProfile = user
                )
            } else {
                Spacer(modifier = Modifier.width(40.dp))
            }
            Column {
                if (prevMessage == null || prevMessage.authorId != user.userId) {
                    Text(
                        member.nickname ?: user.displayName ?: user.userName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Start,
                        color = if (role != null) Color(
                            android.graphics.Color.parseColor(
                                role.color
                            )
                        ) else MaterialTheme.colorScheme.onBackground
                    )
                }
                if (message.content!!.isNotBlank()) {
                    val textStyle = SpanStyle(
                        fontSize = 18.sp
                    )
                    Material3RichText {
                        Markdown(content = message.content)
                    }
                }
                if (message.attachments?.isNotEmpty() == true) {
                    var collapseState by remember { mutableStateOf(message.attachments.size > 2) }
                    if (!collapseState) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalArrangement = Arrangement.spacedBy(
                                5.dp,
                                Alignment.CenterVertically
                            ),
                            horizontalAlignment = Alignment.Start
                        ) {
                            val uriHandler = LocalUriHandler.current
                            message.attachments.forEachIndexed { index, autumnFile ->
                                val url =
                                    RevoltApiModule.getResourceUrl(autumnFile)
                                when (autumnFile.metadata) {
                                    is RevoltFileMetadata.Image -> {
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(
                                                    autumnFile.metadata.width.toFloat() / autumnFile.metadata.height.toFloat(),
                                                    matchHeightConstraintsFirst = true
                                                )
                                                .clip(MaterialTheme.shapes.large)
                                                .height(200.dp)
                                                .clickable {
                                                    if (url != null)
                                                        uriHandler.openUri(url)
                                                }
                                        ) {
                                            AsyncImage(
                                                model = url,
                                                contentDescription = "image attachment $index"
                                            )
                                        }
                                    }

                                    else -> {}
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { collapseState = !collapseState }
                    ) {
                        Text(
                            "${if (collapseState) "Expand" else "Collapse"} attachments",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}