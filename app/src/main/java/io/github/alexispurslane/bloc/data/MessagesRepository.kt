package io.github.alexispurslane.bloc.data

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.toFlowList
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.core.model.RoomId
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

    private val USER_MENTION_REGEX by lazy { Regex("<@([a-zA-Z0-9]+)>") }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun fetchChannelMessages(channelId: String, limit: Int = 50): Flow<List<TimelineEvent>>? {
        return accountsRepository.matrixClient
            ?.room
            ?.getLastTimelineEvents(RoomId(channelId))
            ?.toFlowList(MutableStateFlow(limit))
            ?.flatMapLatest {
                it.flattenFlow()
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun fetchChannelMessagesBefore(channelId: String, lastEvent: TimelineEvent): Flow<List<TimelineEvent>>? {
        return accountsRepository.matrixClient
            ?.room
            ?.getTimelineEvents(RoomId(channelId), startFrom = lastEvent.eventId)
            ?.toFlowList(MutableStateFlow(50))
            ?.flatMapLatest {
                it.flattenFlow()
            }
    }
}

inline fun <reified T> List<Flow<T>>.flattenFlow(): Flow<List<T>> = combine(this@flattenFlow) {
    it.toList()
}