package io.github.alexispurslane.bloc.data

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.local.EmojiMap
import io.github.alexispurslane.bloc.data.local.RevoltAutumnModule
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltMessage
import io.github.alexispurslane.bloc.data.network.models.RevoltMessageSent
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import io.github.alexispurslane.bloc.findIndex
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class RevoltMessagesRepository @Inject constructor(
    private val revoltAccountsRepository: RevoltAccountsRepository,
    private val revoltEmojiRepository: RevoltEmojiRepository
) {
    private val channelMessages: MutableMap<String, SnapshotStateList<RevoltMessage>> =
        mutableMapOf()

    private val existingMessageIds: HashMap<String, Int> = hashMapOf()

    private val USER_MENTION_REGEX by lazy { Regex("<@([a-zA-Z0-9]+)>") }
    private val EMOJI_REGEX by lazy { Regex(":([a-zA-Z0-9_]+):") }
    private suspend fun treatMessage(message: RevoltMessage): RevoltMessage =
        coroutineScope {
            // Doing this makes no sense till all the initial emoji are loaded
            // (we don't have to wait the infinite time that it would take for
            // all emoji that could be added later to be added, because messages
            // will only be expected to render those emoji after they're added,
            // whereas here we're trying to synchronize around a race condition
            // between initial message processing and loading all the emojis that
            // should already exist for those messages, so we need to force messages
            // to be processed *after* the emoji are loaded)
            revoltEmojiRepository.deferredUntilEmojiLoaded.await()

            val userInformation = (message.mentionedIds.orEmpty().plus(
                message.systemEventMessage?.let { USER_MENTION_REGEX.findAll(it.message) }
                    ?.map { it.groupValues[1] }.orEmpty()
            )).map { userId: String ->
                async {
                    when (val u =
                        revoltAccountsRepository.fetchUserInformation(userId)) {
                        is Either.Success -> {
                            val user = u.value
                            user.value.userId to user.value
                        }

                        is Either.Error -> {
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull().toMap()
            val newContent = message.content?.replace(USER_MENTION_REGEX) {
                val user = userInformation[it.groupValues[1]]
                "[@${user?.userName ?: it.value}](bloc://profile/${user?.userId})"
            }?.let { withMentions ->
                val emojiMatches = EMOJI_REGEX.findAll(withMentions)
                val emojis = emojiMatches.map {
                    async {
                        val location =
                            revoltEmojiRepository.getEmoji(it.groupValues[1])
                        if (location != null) {
                            it.groupValues[1] to location
                        } else {
                            null
                        }
                    }
                }.toList().awaitAll().filterNotNull().toMap()
                emojiMatches.fold(withMentions) { acc, matchResult ->
                    val name = matchResult.groupValues[1]
                    val location = emojis[name]
                    acc.replaceRange(
                        matchResult.range,
                        if (location != null)
                            "![${matchResult.value}](${location})"
                        else if (EmojiMap.EMOJI_DICTIONARY[name] != null)
                            EmojiMap.EMOJI_DICTIONARY[name]!!
                        else
                            matchResult.value
                    )
                }
            }?.replace("\n", "\n\n")
            message.systemEventMessage?.let {
                it.message =
                    it.message.replace(USER_MENTION_REGEX) {
                        val user = userInformation[it.groupValues[1]]
                        val id =
                            if (user != null) "@${user.userName}" else it.value
                        "[${id}](bloc://profile/${it.groupValues[1]})"
                    }
            }
            message.copy(
                content = newContent,
                systemEventMessage = message.systemEventMessage
            )
        }

    init {
        GlobalScope.launch(Dispatchers.Default) {
            RevoltWebSocketModule.eventFlow.collect { event ->
                when (event) {
                    is RevoltWebSocketResponse.Message -> {
                        Log.d(
                            "MESSAGE REPO",
                            "New message with id ${event.message.messageId} and content \"${event.message.content}\""
                        )
                        if (!existingMessageIds.contains(event.message.messageId)) {
                            Log.d(
                                "MESSAGE REPO",
                                "Message not already received, adding it"
                            )
                            channelMessages[event.message.channelId]?.apply {
                                add(0, treatMessage(event.message))
                            }
                            existingMessageIds.set(
                                event.message.messageId,
                                (channelMessages[event.message.channelId]?.size
                                    ?: 0) + 1
                            )
                        }
                    }

                    is RevoltWebSocketResponse.MessageDelete -> {
                        existingMessageIds[event.messageId]?.let { reverseIndex ->
                            channelMessages[event.channelId]?.let { channel ->
                                val index = channel.size - (reverseIndex - 1)
                                channel.apply {
                                    removeAt(index)
                                }
                            }
                        }
                    }

                    is RevoltWebSocketResponse.MessageUpdate -> {
                        existingMessageIds[event.messageId]?.let { reverseIndex ->
                            channelMessages[event.channelId]?.let { channel ->
                                val index = channel.size - (reverseIndex - 1)
                                channel.apply {
                                    val old = get(index)
                                    set(
                                        index, old.copy(
                                            messageId = event.data.messageId
                                                ?: old.messageId,
                                            nonce = event.data.nonce
                                                ?: old.nonce,
                                            channelId = event.data.channelId
                                                ?: old.channelId,
                                            authorId = event.data.authorId
                                                ?: old.authorId,
                                            webhook = event.data.webhook
                                                ?: old.webhook,
                                            content = event.data.content
                                                ?: old.content,
                                            systemEventMessage = event.data.systemEventMessage
                                                ?: old.systemEventMessage,
                                            attachments = event.data.attachments
                                                ?: old.attachments,
                                            edited = event.data.edited
                                                ?: old.edited,
                                            embeds = event.data.embeds
                                                ?: old.embeds,
                                            mentionedIds = event.data.mentionedIds
                                                ?: old.mentionedIds,
                                            replyIds = event.data.replyIds
                                                ?: old.replyIds,
                                            reactions = event.data.reactions
                                                ?: old.reactions,
                                            interactions = event.data.interactions
                                                ?: old.interactions,
                                            masquerade = event.data.masquerade
                                                ?: old.masquerade
                                        )
                                    )
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    suspend fun sendMessage(
        channelId: String,
        message: RevoltMessageSent
    ): Either<RevoltMessage, String> {
        val userSession = revoltAccountsRepository.userSessionFlow.first()
        if (userSession.sessionToken == null) {
            return Either.Error(
                "Uh oh! Your user session token is null:You'll have to sign out and sign back in again."
            )
        }
        return try {
            val res = RevoltApiModule.service().sendMessage(
                sessionToken = userSession.sessionToken,
                channelId = channelId,
                message = message
            )
            val body = res.body()!!
            val errorBody = (res.errorBody() ?: res.errorBody())?.string()
            if (res.isSuccessful) {
                Either.Success(body)
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
                "Uh oh! Was unable to send a message to the server: ${e.message}"
            )
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
    ): Either<SnapshotStateList<RevoltMessage>, String> = coroutineScope {
        with(Dispatchers.IO) {
            val userSession = revoltAccountsRepository.userSessionFlow.first()
            if (userSession.sessionToken == null) {
                Either.Error(
                    "Uh oh! Your user session token is null:You'll have to sign out and sign back in again."
                )
            } else {
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
                    val body = res.body()!!.map {
                        async {
                            treatMessage(it)
                        }
                    }.awaitAll()
                    val errorBody =
                        (res.errorBody() ?: res.errorBody())?.string()
                    if (res.isSuccessful) {
                        if (channelMessages.containsKey(channelId)) {
                            if (nearby == null && includeUsers == null && sort != "Relevance" && body.isNotEmpty()) {
                                channelMessages[channelId]!!.apply {
                                    integrateMessages(
                                        this,
                                        body,
                                        before,
                                        after,
                                        sort
                                    )
                                }
                            }
                        } else {
                            channelMessages.getOrPut(
                                channelId,
                                { mutableStateListOf() }).apply {
                                addAll(body)
                            }
                        }
                        val len = channelMessages[channelId]?.size ?: 0
                        existingMessageIds.putAll(
                            body.mapIndexed { i, m -> m.messageId to (body.size - i + len) }
                        )
                        Log.d(
                            "MESSAGE REPO",
                            "Bulk message fetch, adding ids: ${
                                body.map { it.messageId }.toList()
                            }"
                        )
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
                        ?: sortedNewMessages.size
                addAll(index + 1, sortedNewMessages)
            } else if (after != null) {
                val index =
                    findIndex { _, element -> element.messageId == after!! }
                        ?: 0
                addAll(index, sortedNewMessages)
            }
        }
    }

}