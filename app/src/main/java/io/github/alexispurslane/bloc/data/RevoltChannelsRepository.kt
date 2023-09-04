package io.github.alexispurslane.bloc.data

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class RevoltChannelsRepository @Inject constructor(
) {
    val channels: SnapshotStateMap<String, RevoltChannel> = mutableStateMapOf()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            RevoltWebSocketModule.eventFlow.collect {
                onWebSocketEvent(it)
            }
        }
    }

    private fun createChannelMapping(channel: RevoltChannel): Pair<String, RevoltChannel> {
        return when (channel) {
            is RevoltChannel.SavedMessages -> {
                channel.channelId to channel
            }

            is RevoltChannel.DirectMessage -> {
                channel.channelId to channel
            }

            is RevoltChannel.TextChannel -> {
                channel.channelId to channel
            }

            is RevoltChannel.VoiceChannel -> {
                channel.channelId to channel
            }

            is RevoltChannel.Group -> {
                channel.channelId to channel
            }
        }
    }

    private fun onWebSocketEvent(event: RevoltWebSocketResponse): Boolean {
        val repo = this
        channels.apply {
            when (event) {
                is RevoltWebSocketResponse.Ready -> {
                    Log.d("CHANNEL REPO", "Received channels!")
                    putAll(
                        event.channels.associate(repo::createChannelMapping)
                    )
                }

                is RevoltWebSocketResponse.ChannelCreate -> {
                    val mapping = createChannelMapping(event.channel)
                    put(mapping.first, mapping.second)
                }

                is RevoltWebSocketResponse.ChannelDelete -> {
                    remove(event.channelId)
                }

                is RevoltWebSocketResponse.ChannelUpdate -> {
                    Log.w("CHANNEL REPO", "channel edited (not implemented)")
                    /*set(
                        event.channelId,
                        get(event.channelId)?.update(event.data)
                    )*/
                }

                else -> {}
            }
        }
        return true
    }
}