package io.github.alexispurslane.bloc.data

import android.util.Log
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltServer
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class RevoltServersRepository @Inject constructor(
) {
    var _servers: MutableStateFlow<List<RevoltServer>> = MutableStateFlow(
        emptyList()
    )
    val servers: StateFlow<List<RevoltServer>> = _servers.asStateFlow()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            RevoltWebSocketModule.eventFlow.collect {
                onWebSocketEvent(it)
            }
        }
    }

    private fun onWebSocketEvent(event: RevoltWebSocketResponse): Boolean {
        _servers.update { prev ->
            when (event) {
                is RevoltWebSocketResponse.Ready -> {
                    Log.d("SERVER REPO", "Recieved servers!")
                    event.servers
                }
                is RevoltWebSocketResponse.ServerCreate -> {
                    prev.plus(event.server)
                }
                is RevoltWebSocketResponse.ServerDelete -> {
                    prev.filter { it.serverId != event.serverId }
                }
                is RevoltWebSocketResponse.ServerUpdate -> {
                    prev.map {
                        if (it.serverId == event.serverId) {
                            it.copy(
                                event.data.serverId ?: it.serverId,
                                event.data.ownerId ?: it.ownerId,
                                event.data.name ?: it.name,
                                event.data.description ?: it.description,
                                event.data.channelsIds ?: it.channelsIds,
                                event.data.categories ?: it.categories,
                                event.data.systemMessagesConfig ?: it.systemMessagesConfig,
                                event.data.roles ?: it.roles,
                                event.data.defaultPermissions ?: it.defaultPermissions,
                                event.data.icon ?: it.icon,
                                event.data.banner ?: it.banner,
                                event.data.flags ?: it.flags,
                                event.data.nsfw ?: it.nsfw,
                                event.data.analytics ?: it.analytics,
                                event.data.discoverable ?: it.discoverable
                            )
                        } else {
                            it
                        }
                    }
                }
                else -> prev
            }
        }
        return true
    }
}