package io.github.alexispurslane.bloc.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.alexispurslane.bloc.LoadingScreen
import io.github.alexispurslane.bloc.R
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.data.network.models.RevoltFileMetadata
import io.github.alexispurslane.bloc.data.network.models.RevoltMessage
import io.github.alexispurslane.bloc.data.network.models.RevoltServerMember
import io.github.alexispurslane.bloc.data.network.models.RevoltUser
import io.github.alexispurslane.bloc.data.network.models.Role
import io.github.alexispurslane.bloc.ui.models.ServerChannelViewModel

@Composable
fun ChannelViewScreen(
    channelViewModel: ServerChannelViewModel = hiltViewModel()
) {
    val uiState by channelViewModel.uiState.collectAsState()

    if (uiState.channelInfo == null) {
        LoadingScreen()
    } else {
        if (uiState.channelInfo is RevoltChannel.TextChannel) {
            val channelInfo = uiState.channelInfo as RevoltChannel.TextChannel
            Scaffold(
                topBar = {
                    ChannelTopBar(channelInfo)
                },
                content = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(it),
                        verticalArrangement = Arrangement.spacedBy(
                            5.dp,
                            Alignment.Bottom
                        ),
                        horizontalAlignment = Alignment.Start,
                        reverseLayout = true
                    ) {
                        itemsIndexed(
                            uiState.messages,
                            key = { _, it -> it.messageId }) { index, message ->
                            val (user, member) = uiState.users[message.authorId]!!
                            Message(
                                modifier = Modifier.background(
                                    if (message.mentionedIds?.contains(uiState.currentUserId) == true)
                                        Color(0x55FFFF00)
                                    else
                                        Color.Transparent
                                ),
                                message,
                                user,
                                member,
                                role = uiState.serverInfo?.roles?.filterKeys {
                                    member.roles?.contains(
                                        it
                                    ) ?: false
                                }?.values?.minByOrNull { it.rank },
                                prevMessage = if (index > 0) uiState.messages[index - 1] else null
                            )
                        }
                    }
                },
                bottomBar = {
                    MessageBar(channelInfo.name)
                }
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
            Column(
            ) {
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
                    Text(
                        message.content,
                        fontSize = 18.sp,
                    )
                }
                if (message.attachments?.isNotEmpty() == true) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(
                            5.dp,
                            Alignment.Start
                        ),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        message.attachments?.forEachIndexed { index, autumnFile ->
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
                                    ) {
                                        AsyncImage(
                                            model = RevoltApiModule.getResourceUrl(
                                                autumnFile
                                            ),
                                            contentDescription = "image attachment $index"
                                        )
                                    }
                                }

                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MessageBar(channelName: String = "") {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "Add attachment"
                )
            }
            MessageBoxTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp)
                    .height(50.dp),
                placeholder = {
                    Text("Message $channelName...")
                },
                value = "",
                onValueChange = {}
            )
            IconButton(
                onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Add emoji"
                )
            }
            IconButton(
                onClick = { /*TODO*/ }
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelTopBar(channelInfo: RevoltChannel.TextChannel) {
    TopAppBar(
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        title = {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    10.dp,
                    Alignment.Start
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(
                            R.drawable.channel_hashtag
                        ),
                        contentDescription = "channel hashtag icon"
                    )
                }
                Text(
                    channelInfo.name ?: "",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    fontSize = 20.sp,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageBoxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small,
    colors: TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
    // If color is not provided via the text style, use content color as a default
    val textColor = MaterialTheme.colorScheme.onBackground
    val mergedTextStyle = TextStyle(color = textColor)
    val focused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = value,
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, shape)
            .defaultMinSize(
                minWidth = TextFieldDefaults.MinWidth,
                minHeight = TextFieldDefaults.MinHeight
            ),
        onValueChange = onValueChange,
        enabled = enabled,
        readOnly = false,
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        decorationBox = @Composable { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                visualTransformation = visualTransformation,
                innerTextField = innerTextField,
                placeholder = placeholder,
                singleLine = singleLine,
                enabled = enabled,
                isError = isError,
                interactionSource = interactionSource,
                colors = colors,
                contentPadding = PaddingValues(15.dp),
                container = {
                    Box(
                        modifier = Modifier.border(
                            2.dp,
                            if (isError)
                                MaterialTheme.colorScheme.errorContainer
                            else if (focused)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                            OutlinedTextFieldDefaults.shape
                        )
                    )
                }
            )
        }
    )
}
