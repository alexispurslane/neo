package io.github.alexispurslane.bloc.data.network

import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.messageadapter.jackson.JacksonMessageAdapter
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.streamadapter.coroutines.CoroutinesStreamAdapterFactory

object RevoltWebSocketModule {
    private var websocketUrl: String? = null
    private var revoltWebSocketService: RevoltWebSocketService? = null

    fun setWebSocketUrl(newUrl: String): Boolean {
        if (newUrl != websocketUrl) {
            websocketUrl = newUrl
            revoltWebSocketService = null
            return true
        }
        return false
    }

    fun service(): RevoltWebSocketService {
        assert(websocketUrl != null)
        if (revoltWebSocketService == null) {
            revoltWebSocketService = Scarlet.Builder()
                .webSocketFactory(RevoltApiModule.okHttpClient.newWebSocketFactory(
                    websocketUrl!!))
                .addMessageAdapterFactory(JacksonMessageAdapter.Factory())
                .addStreamAdapterFactory(CoroutinesStreamAdapterFactory())
                .build()
                .create()
        }

        return revoltWebSocketService!!
    }
}