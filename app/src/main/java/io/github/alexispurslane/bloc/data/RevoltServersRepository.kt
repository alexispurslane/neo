package io.github.alexispurslane.bloc.data

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltMembersResponse
import io.github.alexispurslane.bloc.data.network.models.RevoltServer
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class RevoltServersRepository @Inject constructor(
    private val revoltAccountsRepository: RevoltAccountsRepository,
) {
    val servers: SnapshotStateMap<String, RevoltServer> =
        mutableStateMapOf()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            RevoltWebSocketModule.eventFlow.collect {
                onWebSocketEvent(it)
            }
        }
    }

    private fun onWebSocketEvent(event: RevoltWebSocketResponse): Boolean {
        servers.apply {
            when (event) {
                is RevoltWebSocketResponse.Ready -> {
                    Log.d("SERVER REPO", "Recieved servers!")
                    putAll(
                        event.servers.associateBy { it.serverId }
                    )
                }

                is RevoltWebSocketResponse.ServerCreate -> {
                    put(event.server.serverId, event.server)
                }

                is RevoltWebSocketResponse.ServerDelete -> {
                    remove(event.serverId)
                }

                is RevoltWebSocketResponse.ServerUpdate -> {
                    val prevServer = get(event.serverId)
                    if (prevServer != null) {
                        set(
                            event.serverId, prevServer.copy(
                                serverId = event.data.serverId
                                    ?: prevServer.serverId,
                                ownerId = event.data.ownerId
                                    ?: prevServer.ownerId,
                                name = event.data.name ?: prevServer.name,
                                description = event.data.description
                                    ?: prevServer.description,
                                channelsIds = event.data.channelsIds
                                    ?: prevServer.channelsIds,
                                categories = event.data.categories
                                    ?: prevServer.categories,
                                systemMessagesConfig = event.data.systemMessagesConfig
                                    ?: prevServer.systemMessagesConfig,
                                roles = event.data.roles ?: prevServer.roles,
                                defaultPermissions = event.data.defaultPermissions
                                    ?: prevServer.defaultPermissions,
                                icon = event.data.icon ?: prevServer.icon,
                                banner = event.data.banner
                                    ?: prevServer.banner,
                                flags = event.data.flags ?: prevServer.flags,
                                nsfw = event.data.nsfw ?: prevServer.nsfw,
                                analytics = event.data.analytics
                                    ?: prevServer.analytics,
                                discoverable = event.data.discoverable
                                    ?: prevServer.discoverable
                            )
                        )
                    }
                }

                else -> {}
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