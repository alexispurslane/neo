package io.github.alexispurslane.bloc.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import io.github.alexispurslane.bloc.MainApplication
import io.github.alexispurslane.bloc.data.local.EmojiMap
import io.github.alexispurslane.bloc.data.local.RevoltAutumnModule
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltEmoji
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import kotlin.streams.toList


enum class EmojiPack(val packName: String) {
    FLUENT_3D("fluent-3d"),
    FLUENT_COLOR("fluent-color"),
    FLUENT_FLAT("fluent-flat"),
    MUTANT("mutant"),
    NOTO("noto"),
    TWEMOJI("twemoji")
}

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class RevoltEmojiRepository @Inject constructor(
    private val application: MainApplication,
) {
    var currentEmojiPack = EmojiPack.MUTANT
    val deferredUntilEmojiLoaded = CompletableDeferred<Unit>()

    // Maps emoji IDs to revolt emoji metadata
    val emoji: SnapshotStateMap<String, RevoltEmoji> = mutableStateMapOf()

    // Maps emoji IDs to emoji filesystem cache locations
    val emojiLocations: SnapshotStateMap<String, String> = mutableStateMapOf()

    // Maps emoji names to emoji IDs
    val emojiIds: SnapshotStateMap<String, String> = mutableStateMapOf()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            RevoltWebSocketModule.eventFlow.collect { event ->
                when (event) {
                    is RevoltWebSocketResponse.Ready -> {
                        Log.d(
                            "EMOJI REPO",
                            "Received ready: ${event.emojis}"
                        )
                        val files = event.emojis.map {
                            async {
                                downloadCustomEmoji(
                                    it.emojiId,
                                )?.let { file -> it.emojiId to file }
                            }
                        }.awaitAll().filterNotNull()
                        withContext(Dispatchers.Main) {
                            emoji.apply {
                                putAll(event.emojis.associateBy { it.emojiId })
                            }
                            emojiIds.apply {
                                putAll(event.emojis.associate { it.name to it.emojiId })
                            }
                            emojiLocations.apply {
                                putAll(files)
                            }
                            deferredUntilEmojiLoaded.complete(Unit)
                            Log.d(
                                "EMOJI REPO",
                                "Loaded ${emojiLocations.size} emoji: ${emojiLocations.keys.toList()}"
                            )
                        }
                    }

                    is RevoltWebSocketResponse.EmojiCreate -> {
                        Log.d(
                            "EMOJI REPO",
                            "Received create: ${event.emoji}"
                        )
                        emoji.apply {
                            put(event.emoji.emojiId, event.emoji)
                        }
                        launch {
                            val file = downloadCustomEmoji(event.emoji.name)
                            if (file != null) {
                                emojiLocations.apply {
                                    put(event.emoji.emojiId, file)
                                }
                                emoji.apply {
                                    put(event.emoji.emojiId, event.emoji)
                                }
                                emojiIds.apply {
                                    put(event.emoji.name, event.emoji.emojiId)
                                }
                            }
                        }
                    }

                    is RevoltWebSocketResponse.EmojiDelete -> {
                        emojiLocations.apply {
                            remove(event.emojiId)
                        }
                        emoji.apply {
                            remove(event.emojiId)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    fun emojiUnicodeToCodepoint(input: String): String {
        if (input.length == 1) {
            return input.codePointAt(0).toString(16)
        } else {
            val pairs = input.codePoints().toList().mapIndexed { i, c ->
                if (c in 0xd800..0xdbff) {
                    val c2 = input.codePointAt(i + 1)
                    if (c2 in 0xdc00..0xdfff) {
                        (c - 0xd800) + 0x400 +
                                (c2 - 0xdc00) +
                                0x10000
                    } else {
                        null
                    }
                } else {
                    c
                }
            }.filterNotNull()

            return pairs.joinToString("-") { it.toString(16) }
        }
    }

    suspend fun getEmoji(
        emojiIdOrName: String
    ): String? {
        val emojiId =
            emojiIds[emojiIdOrName] ?: emojiIdOrName
        val location =
            // Emoji referred to by name (so it's local on the server)
            emojiLocations[emojiId]
            // One last check just to see if maybe it's referred to by ID but on this server
                ?: emojiLocations[emoji[emojiId]?.name]
                // Well, it's not on this server, or else we'd have it in our database, so it must be referred to by ID and from some other server, so go download it, or just return its location if already downloaded
                ?: downloadCustomEmoji(emojiId)
                // Well, then, its one of Revolt's built in, but static, emoji!
                ?: EmojiMap.REVOLT_CUSTOM_BUILTIN_EMOJI[emojiId]?.let { "https://dl.insrt.uk/projects/revolt/emotes/$it" }
        Log.d(
            "EMOJI REPO",
            "Got emoji $emojiIdOrName, location: $location"
        )
        return location
    }

    suspend fun downloadCustomEmoji(
        emojiId: String,
        urlBase: String = "${RevoltAutumnModule.autumnUrl}/emojis",
    ): String? {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                val cacheDir = application.cacheDir.absolutePath
                with(File(cacheDir, "emoji-$emojiId")) {
                    if (!exists()) {
                        val url =
                            URL("$urlBase/$emojiId")
                        val bitmap =
                            with(url.openConnection() as HttpsURLConnection) {
                                requestMethod = "GET"
                                doInput = true

                                Bitmap.createScaledBitmap(
                                    BitmapFactory.decodeStream(inputStream),
                                    64,
                                    64,
                                    false
                                )
                            }
                        val out = FileOutputStream(this)
                        bitmap.compress(
                            Bitmap.CompressFormat.PNG,
                            50,
                            out
                        )
                        out.flush()
                    }
                }
                val file = "$cacheDir/emoji-$emojiId"
                file
            } catch (e: Exception) {
                if (e !is FileNotFoundException) {
                    Log.e("EMOJI REPO", "Cannot download emoji $emojiId: $e")
                }
                null
            }
        }
    }
}