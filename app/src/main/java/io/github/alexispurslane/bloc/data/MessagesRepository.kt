package io.github.alexispurslane.bloc.data

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import io.github.alexispurslane.bloc.ui.composables.screens.Logo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.Timeline
import net.folivo.trixnity.client.room.TimelineEventAggregation
import net.folivo.trixnity.client.room.getTimelineEventReactionAggregation
import net.folivo.trixnity.client.room.toFlowList
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.UnknownEventContent
import net.folivo.trixnity.core.model.events.m.ReactionEventContent
import net.folivo.trixnity.core.model.events.m.RelatesTo
import net.folivo.trixnity.core.model.events.m.room.RedactionEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class MessagesRepository @Inject constructor(
    private val accountsRepository: AccountsRepository
) {
    var _channelMessages: MutableMap<RoomId, Flow<List<TimelineEvent>>> =
        mutableMapOf()
        private set
    val channelMessages
        get(): Map<RoomId, Flow<List<TimelineEvent>>> = _channelMessages

    val messageReactions: SnapshotStateMap<EventId, MutableStateFlow<Map<String, Map<EventId, UserId>>>> = mutableStateMapOf()

    private val USER_MENTION_REGEX by lazy { Regex("@([a-z0-9._=\\-/+]+):(?=.{1,255}\$)[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?(?:\\.[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?)*\\.?") }

    private fun treatMessages(messages: List<TimelineEvent>): List<TimelineEvent> {
        return messages.fold<TimelineEvent, MutableMap<String, TimelineEvent>>(mutableMapOf()) { acc, message ->
            val content = message.content?.getOrNull()
            Log.d("Message Repository", message.toString())
            when (content) {
                is RoomMessageEventContent -> {
                    acc[message.eventId.full] = message
                }
                is UnknownEventContent -> {
                    if (content.eventType == "m.sticker") {
                        acc[message.eventId.full] = message
                    }
                }

                is ReactionEventContent -> {
                    val relates = content.relatesTo
                    if (relates != null) {
                        val eid = content.relatesTo?.eventId
                        val reactions = messageReactions[eid]
                        val pair = (message.eventId to message.sender)
                        if (reactions != null) {
                            reactions.update {
                                val entry =
                                    (it[relates.key] ?: mapOf()) + pair
                                it + (relates.key!! to entry)
                            }
                        } else {
                            messageReactions[eid!!] = MutableStateFlow(
                                mapOf(relates.key!! to mapOf(pair))
                            )
                        }
                    }
                }

                else -> { }
            }
            acc
        }.values.toList()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun fetchChannelMessages(channelId: String, limit: Int = 50): Flow<List<TimelineEvent>>? {
        return channelMessages[RoomId(channelId)] ?: accountsRepository.matrixClient
            ?.room
            ?.getLastTimelineEvents(RoomId(channelId))
            ?.toFlowList(MutableStateFlow(limit))
            ?.flatMapLatest {
                it.flattenFlow()
            }?.map { treatMessages(it) }
            ?.let { _channelMessages.put(RoomId(channelId), it); it }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun fetchChannelMessagesBefore(channelId: String, lastEvent: TimelineEvent): Flow<List<TimelineEvent>>? {
        return _channelMessages.compute(RoomId(channelId)) { k, messages ->
            accountsRepository.matrixClient
                ?.room
                ?.getTimelineEvents(RoomId(channelId), startFrom = lastEvent.eventId)
                ?.toFlowList(MutableStateFlow(50))
                ?.flatMapLatest {
                    it.flattenFlow()
                }?.map { treatMessages(it) }?.combine(messages ?: flowOf(listOf())) { oldMessages, newMessages ->
                    // oldMessages means old chronologically, in the timeline, so oldMessages is the "new" messages from the perspective of what we're having to add
                    newMessages + treatMessages(oldMessages.drop(1))
                }
        }
    }
}

inline fun <reified T> List<Flow<T>>.flattenFlow(): Flow<List<T>> = combine(this@flattenFlow) {
    it.toList()
}