package io.github.alexispurslane.bloc.data

import android.util.Log
import io.github.alexispurslane.bloc.ui.composables.screens.Logo
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.Timeline
import net.folivo.trixnity.client.room.toFlowList
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.RelatesTo
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

    private val USER_MENTION_REGEX by lazy { Regex("@([a-z0-9._=\\-/+]+):(?=.{1,255}\$)[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?(?:\\.[0-9A-Za-z](?:(?:[0-9A-Za-z]|-){0,61}[0-9A-Za-z])?)*\\.?") }

    private fun treatMessages(messages: List<TimelineEvent>): List<TimelineEvent> {
        return messages.fold<TimelineEvent, MutableMap<String, TimelineEvent>>(mutableMapOf()) { acc, message ->
            acc[message.eventId.full] = message
            when (message.content?.getOrNull()) {
                is RoomMessageEventContent -> {
                    acc
                }
                else -> {
                    acc
                }
            }
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