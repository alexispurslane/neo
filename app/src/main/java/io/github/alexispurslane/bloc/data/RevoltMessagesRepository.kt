package io.github.alexispurslane.bloc.data

import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltMessage
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class RevoltMessagesRepository @Inject constructor(
    private val revoltAccountsRepository: RevoltAccountsRepository
) {
    private var messagesCache: MutableMap<String, List<RevoltMessage>> =
        mutableMapOf()
    private var _messages: MutableSharedFlow<RevoltMessage> =
        MutableSharedFlow(replay = 10, extraBufferCapacity = 10)
    val messages: SharedFlow<RevoltMessage> = _messages.asSharedFlow()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            RevoltWebSocketModule.eventFlow.collect { event ->
                when (event) {
                    is RevoltWebSocketResponse.Message -> {
                        messagesCache[event.message.channelId]?.plus(event.message)
                        _messages.tryEmit(event.message)
                    }

                    else -> {}
                }
            }
        }
    }

    suspend fun fetchMessages(
        channelId: String,
        limit: Int? = null,
        before: String? = null,
        after: String? = null,
        sort: String? = null,
        nearby: String? = null,
        includeUsers: Boolean? = null
    ): Either<List<RevoltMessage>, String> {
        val messages = messagesCache.get(channelId)
        if (messages != null) {
            return Either.Success(messages)
        }

        val userSession = revoltAccountsRepository.userSessionFlow.first()
        if (userSession.sessionToken != null) {
            try {
                val res = RevoltApiModule.service().fetchMessages(
                    userSession.sessionToken,
                    channelId,
                    limit,
                    before,
                    after,
                    sort,
                    nearby,
                    includeUsers
                )
                return if (res.isSuccessful) {
                    messagesCache[channelId] = res.body()!!
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
                return Either.Error("Uh oh! Was unable to send request for messages to the server: ${e.message}")
            }
        } else {
            return Either.Error("Uh oh! Your user session token is null:You'll have to sign out and sign back in again.")
        }
    }
}