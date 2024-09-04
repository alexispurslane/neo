package io.github.alexispurslane.neo.data

import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.flattenValues
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.RoomService
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.RoomUser
import net.folivo.trixnity.client.store.type
import net.folivo.trixnity.client.user
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.m.room.CreateEventContent
import net.folivo.trixnity.core.model.events.m.space.ChildEventContent
import net.folivo.trixnity.core.model.events.m.space.ParentEventContent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

sealed class RoomTree {
    data class Space(val space: Room, val children: MutableMap<RoomId, RoomTree>): RoomTree()
    data class Channel(val room: Room): RoomTree()
}

suspend fun RoomService.getChildren(roomId: RoomId) =
    getAllState(roomId, eventContentClass = ChildEventContent::class).first().keys

suspend fun RoomService.getParents(roomId: RoomId) =
    getAllState(roomId, eventContentClass = ParentEventContent::class).first().keys

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class RoomsRepository @Inject constructor(
    private val accountsRepository: AccountsRepository,
) {
    private val _rooms: MutableStateFlow<Map<RoomId, RoomTree>> = MutableStateFlow(
        mutableMapOf()
    )
    val rooms: StateFlow<Map<RoomId, RoomTree>> = _rooms.asStateFlow()
    // room IDs that have been seen already *in a hierarchy* and so take precedence
    val roomDirectory: MutableStateFlow<Map<RoomId, Room>> = MutableStateFlow(mapOf())
    var seenRooms: MutableSet<RoomId> = mutableSetOf()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            accountsRepository.matrixClientFlow.filterNotNull().distinctUntilChanged().collect { matrixClient ->
                Log.d("Room Repository", "non-null matrixclient, using that")
                if (roomDirectory.value.isEmpty()) {
                    matrixClient.room.getAll().flattenValues(throttle = 3.seconds).collectLatest {
                        Log.d("Room Repository", "Got rooms, building room hiearchy")
                        roomDirectory.emit(it.associateBy { it.roomId })
                        val roomsMap = mutableMapOf<RoomId, RoomTree>()
                        it.forEach { room ->
                            if (!seenRooms.contains(room.roomId)) {
                                buildHierarchy(matrixClient, room, roomsMap)?.let {
                                    roomsMap[room.roomId] = it
                                }
                            }
                        }
                        _rooms.update { roomsMap }
                        Log.d("Room Repository", "Done building room hierarchy")
                    }
                }
            }
        }
    }

    private suspend fun buildHierarchy(matrixClient: MatrixClient, room: Room, roomsMap: MutableMap<RoomId, RoomTree>): RoomTree? = coroutineScope {
        if (room.type == CreateEventContent.RoomType.Space) {
            val children = matrixClient.room.getChildren(room.roomId).map { childId ->
                async {
                    (roomsMap.remove(RoomId(childId)) ?: matrixClient.room.getById(RoomId(childId)).first()?.let { childRoom ->
                        Log.d("Room Repository", "${room.name?.explicitName}/${childRoom.name?.explicitName}")
                        seenRooms.add(childRoom.roomId)
                        buildHierarchy(matrixClient, childRoom, roomsMap)
                    })?.let { RoomId(childId) to it }
                }
            }.awaitAll().filterNotNull().toMap().toMutableMap()

            RoomTree.Space(space = room, children = children)
        } else {
            RoomTree.Channel(room)
        }
    }

    suspend fun fetchSpaceMembers(spaceId: RoomId): Flow<List<RoomUser>>? {
        accountsRepository.matrixClient?.user?.loadMembers(spaceId)
        return accountsRepository.matrixClient?.user?.getAll(spaceId)?.flattenValues()
    }
}