package io.github.alexispurslane.bloc.data.network

import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import android.util.Log
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class RevoltChannelsRepository @Inject constructor(
) {
    var _channels: MutableStateFlow<Map<String, RevoltChannel>> = MutableStateFlow(
        emptyMap()
    )
    val channels: StateFlow<Map<String, RevoltChannel>> = _channels.asStateFlow()

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
        _channels.update { prev ->
            when (event) {
                is RevoltWebSocketResponse.Ready -> {
                    Log.d("CHANNEL REPO", "Received channels!")
                    event.channels.associate(this::createChannelMapping)
                }
                is RevoltWebSocketResponse.ChannelCreate -> {
                    prev.plus(createChannelMapping(event.channel))
                }
                is RevoltWebSocketResponse.ChannelUpdate -> {
                    Log.w("CHANNEL REPO", "channel edited (not implemented)")
                    prev
                }
                else -> prev
            }
        }
        return true
    }
}