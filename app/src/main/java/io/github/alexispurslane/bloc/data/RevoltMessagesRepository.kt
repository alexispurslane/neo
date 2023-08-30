package io.github.alexispurslane.bloc.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltMessage
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import io.github.alexispurslane.bloc.findIndex
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
class RevoltMessagesRepository @Inject constructor(
    private val revoltAccountsRepository: RevoltAccountsRepository
) {
    var _channelMessages: MutableMap<String, SnapshotStateList<RevoltMessage>> =
        mutableMapOf()
        private set
    val channelMessages
        get(): Map<String, SnapshotStateList<RevoltMessage>> = _channelMessages

    init {
        GlobalScope.launch(Dispatchers.IO) {
            RevoltWebSocketModule.eventFlow.collect { event ->
                when (event) {
                    is RevoltWebSocketResponse.Message -> {
                        _channelMessages[event.message.channelId]?.apply {
                            add(0, event.message)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    suspend fun fetchChannelMessages(
        channelId: String,
        limit: Int? = null,
        before: String? = null,
        after: String? = null,
        sort: String? = null,
        nearby: String? = null,
        includeUsers: Boolean? = null
    ): Either<SnapshotStateList<RevoltMessage>, String> {
        val userSession = revoltAccountsRepository.userSessionFlow.first()
        if (userSession.sessionToken == null) {
            return Either.Error(
                "Uh oh! Your user session token is null:You'll have to sign out and sign back in again."
            )
        }

        return try {
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
            val body = res.body()!!
            val errorBody = (res.errorBody() ?: res.errorBody())?.string()
            if (res.isSuccessful) {
                if (_channelMessages.containsKey(channelId)) {
                    if (nearby == null && includeUsers == null && sort != "Relevance") {
                        _channelMessages[channelId]!!.apply {
                            integrateMessages(this, body, before, after, sort)
                        }
                    }
                } else {
                    _channelMessages.getOrPut(
                        channelId,
                        { mutableStateListOf() }).apply {
                        addAll(body)
                    }
                }
                Either.Success(channelMessages[channelId]!!)
            } else if (errorBody != null) {
                val jsonObject = JSONObject(errorBody.trim())
                Either.Error(
                    "Uh oh! ${res.message()}:The server error was '${
                        jsonObject.getString(
                            "type"
                        )
                    }'"
                )
            } else {
                Either.Error(
                    "Uh oh! The server returned an error:${res.message()}"
                )
            }
        } catch (e: Exception) {
            Either.Error(
                "Uh oh! Was unable to send request for messages to the server: ${e.message}"
            )
        }
    }

    private fun integrateMessages(
        revoltMessages: SnapshotStateList<RevoltMessage>,
        newMessages: List<RevoltMessage>,
        before: String?,
        after: String?,
        sort: String?,
    ) {
        val sortedNewMessages = when (sort) {
            "Latest" -> {
                newMessages.reversed()
            }

            "Oldest" -> {
                newMessages
            }

            else -> {
                newMessages
            }
        }
        revoltMessages.apply {
            if (before != null) {
                val index =
                    findIndex { _, element -> element.messageId == before }
                        ?: size
                addAll(index, sortedNewMessages)
            } else if (after != null) {
                val index =
                    findIndex { _, element -> element.messageId == after!! }
                        ?: 0
                addAll(index + 1, sortedNewMessages)
            }
        }
    }

}