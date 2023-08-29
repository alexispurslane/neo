package io.github.alexispurslane.bloc.data.network

import android.util.Log
import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.databind.json.JsonMapper
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketRequest
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlin.time.Duration.Companion.seconds

typealias WebSocketSubscriber = (RevoltWebSocketResponse) -> Boolean

object RevoltWebSocketModule {
    var websocketUrl: String? = null
        private set
    private var revoltWebSocket: WebSocket? = null
    private var _eventFlow: MutableSharedFlow<RevoltWebSocketResponse> = MutableSharedFlow(replay = 3, extraBufferCapacity = 3)
    val eventFlow: SharedFlow<RevoltWebSocketResponse> = _eventFlow.asSharedFlow()
    private var keepAliveJob: Job? = null
    private var sessionToken: String? = null

    private val okHttpClient by lazy {
        OkHttpClient().newBuilder().build()
    }

    private val jsonMapper by lazy {
        JsonMapper()
    }

    fun setWebSocketUrlAndToken(newUrl: String, token: String): Boolean {
        if (newUrl != websocketUrl || sessionToken != token || revoltWebSocket == null) {
            websocketUrl = newUrl
            sessionToken = token
            revoltWebSocket = null
            return true
        }
        return false
    }

    fun send(event: RevoltWebSocketRequest): Boolean {
        Log.d("WEBSOCKET", "sending: ${event.toString()}")
        return service()!!.send(jsonMapper.writeValueAsString(event))
    }

    fun authenticate(token: String): Boolean {
        Log.d("WEBSOCKET", "authenticating websocket")
        return send(RevoltWebSocketRequest.Authenticate(sessionToken = token))
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun createWebSocket() {
        revoltWebSocket = okHttpClient.newWebSocket(Request.Builder().url(websocketUrl!!).build(), object : WebSocketListener() {
            override fun onClosed(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {
                super.onClosed(webSocket, code, reason)

                Log.d("WEBSOCKET", "WebSocket closed.")
                revoltWebSocket = null
            }

            override fun onClosing(
                webSocket: WebSocket,
                code: Int,
                reason: String
            ) {
                super.onClosing(webSocket, code, reason)
                Log.d("WEBSOCKET", "WebSocket closing...")
            }

            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?
            ) {
                super.onFailure(webSocket, t, response)
                Log.e("WEBSOCKET", "WebSocket failed: ${t.message}")
                revoltWebSocket = null
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                Log.d("WEBSOCKET", "WebSocket opened.")
                authenticate(sessionToken!!)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                val event = jsonMapper.readValue(text, RevoltWebSocketResponse::class.java)
                Log.d("WEBSOCKET", "receiving: $event")
                _eventFlow.tryEmit(event)
            }

            override fun onMessage(
                webSocket: WebSocket,
                bytes: okio.ByteString
            ) {
                super.onMessage(webSocket, bytes)
                onMessage(webSocket, bytes.toString())
            }
        })

        keepAliveJob?.cancel("WebSocket restarted")
        keepAliveJob = GlobalScope.launch(Dispatchers.IO) {
            while (revoltWebSocket != null) {
                delay(25.seconds)
                send(RevoltWebSocketRequest.Ping(timestamp = System.currentTimeMillis()))
            }
        }

        Log.d("WEBSOCKET", "WebSocket (re)created.")
    }

    fun service(): WebSocket? {
        if (websocketUrl != null
            && sessionToken != null
            && revoltWebSocket == null) {
            createWebSocket()
        }
        return revoltWebSocket
    }
}