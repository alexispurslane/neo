package io.github.alexispurslane.bloc.ui.composables.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.alexispurslane.bloc.R
import io.github.alexispurslane.bloc.data.local.RevoltAutumnModule
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.ui.composables.emojipicker.ComposeEmojiPickerBottomSheetUI
import io.github.alexispurslane.bloc.ui.theme.AppFont

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun MessageBar(
    channelName: String = "",
    value: String = "",
    serverEmoji: List<String> = listOf(),
    onValueChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {}
) {
    Box(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment
            IconButton(
                onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = "Add attachment"
                )
            }

            // Message content
            var modalEditDialogue by rememberSaveable { mutableStateOf(false) }

            MessageBoxTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(5.dp)
                    .heightIn(50.dp, 200.dp),
                placeholder = {
                    Text("Message $channelName...")
                },
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = true,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default
                ),
                textStyle = TextStyle(fontSize = 15.sp)
            )

            LaunchedEffect(value.length) {
                modalEditDialogue = value.length > 500
            }

            if (modalEditDialogue) {
                LongFormEditor(
                    value = value,
                    onValueChange = onValueChange,
                    onDismissRequest = { modalEditDialogue = false },
                    channelName = channelName
                )
            }

            // Emoji
            var modalEmojiDialog by rememberSaveable { mutableStateOf(false) }
            IconButton(
                onClick = { modalEmojiDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Add emoji"
                )
            }

            if (modalEmojiDialog) {
                var sheetState = rememberModalBottomSheetState()
                ModalBottomSheet(
                    sheetState = sheetState,
                    onDismissRequest = { modalEmojiDialog = false }
                ) {
                    ComposeEmojiPickerBottomSheetUI(
                        onEmojiClick = { emoji ->
                            onValueChange(value + ":${emoji.character}:");
                            modalEmojiDialog = false
                        },
                        serverEmoji = serverEmoji
                    )
                }
            }

            IconButton(
                onClick = onSubmit
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
fun LongFormEditor(
    value: String,
    onValueChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    channelName: String = ""
) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        sheetState = bottomSheetState,
        onDismissRequest = { onDismissRequest() }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            ProvideTextStyle(value = MaterialTheme.typography.headlineLarge) {
                Text(
                    "Long-form text input",
                    modifier = Modifier.padding(bottom = 5.dp),
                    textAlign = TextAlign.Start
                )
            }
            MessageBoxTextField(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                placeholder = {
                    Text("Message $channelName...")
                },
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrect = true,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Default
                ),
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Serif
                )
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelTopBar(channelInfo: RevoltChannel.TextChannel) {
    TopAppBar(
        colors = topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
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
                    if (channelInfo.icon != null) {
                        AsyncImage(
                            model = RevoltAutumnModule.getResourceUrl(
                                LocalContext.current, channelInfo.icon
                            ),
                            contentDescription = "Channel Icon",
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                R.drawable.channel_hashtag
                            ),
                            contentDescription = "channel hashtag icon"
                        )
                    }
                }

                ProvideTextStyle(value = MaterialTheme.typography.titleLarge) {
                    Text(
                        channelInfo.name,
                        modifier = Modifier.padding(top = 3.dp),
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    textStyle: TextStyle? = null
) {
    // If color is not provided via the text style, use content color as a default
    val textColor = MaterialTheme.colorScheme.onBackground
    val mergedTextStyle =
        TextStyle(color = textColor, fontFamily = AppFont.Rubik).merge(
            textStyle
        )
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