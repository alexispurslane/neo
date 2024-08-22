package io.github.alexispurslane.bloc.data

import io.github.alexispurslane.bloc.Either
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.type
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class ServersRepository @Inject constructor(
    private val accountsRepository: AccountsRepository,
) {
    private var _spaces: MutableStateFlow<Map<RoomId, Room>> =
        MutableStateFlow(
            emptyMap()
        )
    val spaces: StateFlow<Map<RoomId, Room>> = _spaces.asStateFlow()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            accountsRepository.matrixClient?.room?.getAll()?.flattenValues()?.collect { rooms ->
                _spaces.update {
                    rooms.filter { it.type == CreateEventContent.RoomType.Space }
                        .associateBy { it.roomId }
                }
            }
        }
    }

    suspend fun fetchSpaceMembers(spaceId: RoomId): Flow<List<RoomUser>>? {
        accountsRepository.matrixClient?.user?.loadMembers(spaceId)
        return accountsRepository.matrixClient?.user?.getAll(spaceId)?.flattenValues()
    }
}