package io.github.alexispurslane.bloc.ui.composables.emojipicker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import emoji.core.datasource.EmojiDataSource
import emoji.core.datasource.EmojiDataSourceImpl
import io.github.alexispurslane.bloc.data.local.EmojiMap
import io.github.alexispurslane.bloc.ui.composables.emojipicker.utils.isEmojiRenderable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Float.max
import kotlin.math.ceil
import kotlin.math.floor

val black = Color(0xFF1C2020)
val white = Color(0xFFF2F7F7)
val gray = Color(0xFF848686)

/**
 * Mandatory parameters - Only [onEmojiClick] is mandatory parameter.
 *
 * @param onEmojiClick Click handler
 *
 * @param backgroundColor Whether the incoming min constraints should be passed to content.
 * @param onEmojiLongClick Whether the incoming min constraints should be passed to content.
 **/

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ComposeEmojiPickerBottomSheetUI(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    searchBarColor: Color = MaterialTheme.colorScheme.primaryContainer,

    groupTitleTextColor: Color = MaterialTheme.colorScheme.onBackground,
    groupTitleTextStyle: TextStyle = MaterialTheme.typography.headlineMedium,

    emojiFontSize: TextUnit = 18.sp,
    searchText: String = "",
    onEmojiClick: (emoji: Emoji) -> Unit,
    onEmojiLongClick: ((emoji: Emoji) -> Unit)? = null,
    updateSearchText: ((updatedSearchText: String) -> Unit)? = null,
    serverEmoji: List<String>,
) {
    val context = LocalContext.current
    var isLoading by remember {
        mutableStateOf(false)
    }
    var emojis by remember {
        mutableStateOf(emptyList<Emoji>())
    }
    val emojiGroups by remember(
        key1 = emojis,
        key2 = searchText,
    ) {
        mutableStateOf(emojis.filter { emoji ->
            if (searchText.isBlank()) {
                true
            } else {
                emoji.unicodeName.contains(
                    other = searchText,
                )
            }
        }.groupBy { emoji ->
            emoji.group
        }.filter { (_, emojis) ->
            emojis.isNotEmpty()
        })
    }

    val firstEmoji = emojiGroups.values.firstOrNull()?.firstOrNull()?.character
    val emojiWidth = rememberEmojiWidth(
        firstEmoji = firstEmoji,
        emojiFontSize = emojiFontSize,
    )

    LaunchedEffect(
        key1 = Unit,
    ) {
        CoroutineScope(Dispatchers.Default).launch {
            val emojiDataSource: EmojiDataSource = EmojiDataSourceImpl(
                cacheFile = File(context.cacheDir, "http_cache"),
            )
            withContext(Dispatchers.Main) {
                isLoading = true
                emojis = emojiDataSource.getAllEmojis().map {
                    Emoji(it)
                }.filter {
                    isEmojiRenderable(it)
                } + EmojiMap.REVOLT_CUSTOM_BUILTIN_EMOJI.map {
                    Emoji(
                        it.key,
                        "",
                        "Revolt Emoji",
                        "Built-In",
                        it.key
                    )
                } + serverEmoji.map {
                    Emoji(
                        it,
                        "",
                        "Revolt Emoji",
                        "Server-Local",
                        it
                    )
                }
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = backgroundColor,
            )
            .defaultMinSize(
                minHeight = 100.dp,
            ),
    ) {
        ComposeEmojiPickerSearchBar(
            backgroundColor = backgroundColor,
            searchBarColor = searchBarColor,
            text = searchText,
            onValueChange = updateSearchText ?: {},
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                if (isLoading) {
                    item {
                        ComposeEmojiPickerLoadingUI()
                    }
                } else if (firstEmoji == null || emojiWidth == null) {
                    item {
                        ComposeEmojiPickerEmptyUI()
                    }
                } else {
                    val (columnCount, itemPadding) = getColumnData(
                        maxColumnWidth = maxWidth,
                        emojiWidth = emojiWidth,
                    )
                    emojiGroups.map { (group, emojis) ->
                        stickyHeader {
                            ComposeEmojiPickerGroupTitle(
                                backgroundColor = backgroundColor,
                                titleTextColor = groupTitleTextColor,
                                titleText = "$group (${emojis.size})",
                                titleTextStyle = groupTitleTextStyle,
                            )
                        }
                        emojis.chunked(
                            size = columnCount,
                        ).map {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    it.forEach { emoji ->
                                        ComposeEmojiPickerEmojiUI(
                                            modifier = Modifier
                                                .padding(
                                                    horizontal = itemPadding,
                                                ),
                                            emojiCharacter = emoji.character,
                                            onClick = {
                                                onEmojiClick(emoji)
                                            },
                                            onLongClick = {
                                                onEmojiLongClick?.invoke(emoji)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getColumnData(
    maxColumnWidth: Dp,
    emojiWidth: Dp,
): Pair<Int, Dp> {
    val emojiWidthWithPadding = emojiWidth + (3.dp * 2)
    val columnCount = (maxColumnWidth / (emojiWidthWithPadding)).toInt()
    val ceilEmojiWidth = ceil(emojiWidthWithPadding.value).dp
    val itemPadding =
        max(
            floor(((maxColumnWidth - (ceilEmojiWidth * columnCount)) / (2 * columnCount)).value),
            0.0F
        ).dp
    return Pair(columnCount, itemPadding)
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberEmojiWidth(
    firstEmoji: String?,
    emojiFontSize: TextUnit
): Dp? {
    if (firstEmoji == null) {
        return null
    }
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    return remember {
        with(density) {
            firstEmoji.run {
                textMeasurer.measure(
                    text = firstEmoji,
                    style = TextStyle(
                        fontSize = emojiFontSize,
                    ),
                ).size.width.toDp()
            }
        }
    }
}