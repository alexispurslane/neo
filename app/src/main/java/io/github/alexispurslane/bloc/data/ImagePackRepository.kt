@file:Suppress("OPT_IN_USAGE")

package io.github.alexispurslane.bloc.data

import android.media.Image
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import io.github.alexispurslane.bloc.data.models.ImagePackEventContent
import io.github.alexispurslane.bloc.data.models.PackObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import net.folivo.trixnity.client.room
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.UnknownEventContent
import javax.inject.Inject
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Unknown

class ImagePackRepository @Inject constructor(
    private val roomsRepository: RoomsRepository,
    private val accountsRepository: AccountsRepository
) {
    val imagePacksByRoom: SnapshotStateMap<RoomId, PackObject> = mutableStateMapOf()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            roomsRepository.rooms.collectLatest { rooms ->
                rooms.keys.forEach { roomId ->
                    launch {
                        accountsRepository.matrixClient!!.room.getAllState(
                            roomId = roomId,
                            eventContentClass = ImagePackEventContent::class
                        ).collect {
                            Log.d("Image Pack Repo", it.toString())
                        }
                    }
                }
            }
        }
    }
}