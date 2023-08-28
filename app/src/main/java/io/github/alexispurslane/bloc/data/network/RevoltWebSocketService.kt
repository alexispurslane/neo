package io.github.alexispurslane.bloc.data.network

import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketRequest
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.channels.ReceiveChannel

interface RevoltWebSocketService {
    @Receive
    fun observeOnConnectionOpenedEvent(): ReceiveChannel<WebSocket.Event>

    @Receive
    fun observeEvent(): ReceiveChannel<RevoltWebSocketResponse>

    @Send
    fun send(message: RevoltWebSocketRequest): Boolean
}