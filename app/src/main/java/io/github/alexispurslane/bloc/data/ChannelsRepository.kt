package io.github.alexispurslane.bloc.data

import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.type
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import okhttp3.internal.toImmutableList
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
@Singleton
class ChannelsRepository @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val serversRepository: ServersRepository
) {
    private lateinit var _channels: Flow<Map<RoomId, Map<RoomId, Room>>>
    lateinit var channels: StateFlow<Map<RoomId, Map<RoomId, Room>>>
        private set

    init {
        GlobalScope.launch(Dispatchers.IO) {
            _channels = serversRepository.spaces.mapLatest { spaces ->
                spaces.map {
                    it.key to (
                            accountsRepository.matrixClient?.api?.room
                                ?.getHierarchy(it.key, "")
                                ?.getOrNull()
                                ?.rooms
                                ?.map {
                                    accountsRepository.matrixClient?.room
                                        ?.getById(it.roomId)!!
                                        .filterNotNull()
                                        .last()
                                }?.associateBy { it.roomId }
                                ?: mapOf())
                }.toMap()
            }
            channels = _channels.stateIn(GlobalScope)
        }
    }
}