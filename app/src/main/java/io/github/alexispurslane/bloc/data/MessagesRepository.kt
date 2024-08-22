package io.github.alexispurslane.bloc.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.findIndex
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.toFlowList
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.client.store.eventId
import net.folivo.trixnity.client.store.sender
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import net.folivo.trixnity.core.model.push.ServerDefaultPushRules
import okhttp3.internal.wait
import org.json.JSONObject
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
}

inline fun <reified T> List<Flow<T>>.flattenFlow(): Flow<List<T>> = combine(this@flattenFlow) {
    it.toList()
}