package io.github.alexispurslane.bloc.data

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltEmoji
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class RevoltEmojiRepository @Inject constructor() {
    val emoji: SnapshotStateMap<String, RevoltEmoji> = mutableStateMapOf()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            RevoltWebSocketModule.eventFlow.collectLatest { event ->
                when (event) {
                    is RevoltWebSocketResponse.Ready -> {
                        emoji.apply {
                            putAll(event.emojis.associateBy { it.emojiId })
                        }
                    }

                    is RevoltWebSocketResponse.EmojiCreate -> {
                        emoji.apply {
                            put(event.emoji.emojiId, event.emoji)
                        }
                    }

                    is RevoltWebSocketResponse.EmojiDelete -> {
                        emoji.apply {
                            remove(event.emojiId)
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}