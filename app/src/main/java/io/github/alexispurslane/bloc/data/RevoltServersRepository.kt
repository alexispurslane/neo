package io.github.alexispurslane.bloc.data

import android.util.Log
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltMembersResponse
import io.github.alexispurslane.bloc.data.network.models.RevoltServer
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class RevoltServersRepository @Inject constructor(
    private val revoltAccountsRepository: RevoltAccountsRepository,
) {
    private var _servers: MutableStateFlow<Map<String, RevoltServer>> =
        MutableStateFlow(
            emptyMap()
        )
    val servers: StateFlow<Map<String, RevoltServer>> = _servers.asStateFlow()

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
                    event.servers.associate {
                        it.serverId to it
                    }
                }
                is RevoltWebSocketResponse.ServerCreate -> {
                    prev.plus(event.server.serverId to event.server)
                }
                is RevoltWebSocketResponse.ServerDelete -> {
                    prev.minus(event.serverId)
                }
                is RevoltWebSocketResponse.ServerUpdate -> {
                    val prevServer = prev[event.serverId]
                    if (prevServer != null) {
                        prev.plus(
                            event.serverId to prevServer.copy(
                                event.data.serverId ?: prevServer.serverId,
                                event.data.ownerId ?: prevServer.ownerId,
                                event.data.name ?: prevServer.name,
                                event.data.description
                                    ?: prevServer.description,
                                event.data.channelsIds
                                    ?: prevServer.channelsIds,
                                event.data.categories ?: prevServer.categories,
                                event.data.systemMessagesConfig
                                    ?: prevServer.systemMessagesConfig,
                                event.data.roles ?: prevServer.roles,
                                event.data.defaultPermissions
                                    ?: prevServer.defaultPermissions,
                                event.data.icon ?: prevServer.icon,
                                event.data.banner ?: prevServer.banner,
                                event.data.flags ?: prevServer.flags,
                                event.data.nsfw ?: prevServer.nsfw,
                                event.data.analytics ?: prevServer.analytics,
                                event.data.discoverable
                                    ?: prevServer.discoverable
                            )
                        )
                    } else {
                        prev
                    }
                }

                else -> prev
            }
        }
        return true
    }

    suspend fun fetchServerMembers(serverId: String): Either<RevoltMembersResponse, String> {
        val userSession = revoltAccountsRepository.userSessionFlow.first()
        if (userSession.sessionToken != null) {
            try {
                val res = RevoltApiModule.service()
                    .fetchServerMembers(userSession.sessionToken, serverId)
                return if (res.isSuccessful) {
                    Either.Success(res.body()!!)
                } else {
                    val errorBody =
                        (res.errorBody() ?: res.errorBody())?.string()
                    if (errorBody != null) {
                        val jsonObject = JSONObject(errorBody.trim())
                        Either.Error(
                            "Uh oh! ${res.message()}:The server error was '${
                                jsonObject.getString(
                                    "type"
                                )
                            }'"
                        )
                    } else {
                        Either.Error("Uh oh! The server returned an error:${res.message()}")
                    }
                }
            } catch (e: Exception) {
                return Either.Error("Uh oh! Was unable to send request for server members to the server: ${e.message}")
            }
        } else {
            return Either.Error("Uh oh! Your user session token is null:You'll have to sign out and sign back in again.")
        }
    }
}