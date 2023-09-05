package io.github.alexispurslane.bloc.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import io.github.alexispurslane.bloc.MainApplication
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class RevoltEmojiRepository @Inject constructor(
    private val application: MainApplication,
) {
    val deferredUntilEmojiLoaded = CompletableDeferred<Unit>()

    val emoji: SnapshotStateMap<String, RevoltEmoji> = mutableStateMapOf()
    val emojiLocations: SnapshotStateMap<String, String> = mutableStateMapOf()

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
                                downloadEmoji(it.emojiId)?.let { path ->
                                    it.name to path
                                }
                            }
                        }.awaitAll().filterNotNull().toMap()
                        withContext(Dispatchers.Main) {
                            emoji.apply {
                                putAll(event.emojis.associateBy { it.name })
                            }
                            emojiLocations.apply {
                                putAll(files)
                            }
                        }
                        deferredUntilEmojiLoaded.complete(Unit)
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
                            val file = downloadEmoji(event.emoji.name)
                            if (file != null) {
                                emojiLocations.apply {
                                    put(event.emoji.name, file)
                                }
                            }
                        }
                    }

                    is RevoltWebSocketResponse.EmojiDelete -> {
                        emoji.apply {
                            remove(event.emojiId)
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    private fun downloadEmoji(emojiId: String): String? {
        return try {
            val cacheDir = application.cacheDir.absolutePath
            with(File(cacheDir, "emoji-$emojiId")) {
                if (!exists()) {
                    val url =
                        URL("${RevoltAutumnModule.autumnUrl}/emojis/$emojiId")
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
            "$cacheDir/emoji-$emojiId"
        } catch (e: Exception) {
            Log.e("EMOJI REPO", "Cannot download emoji: $e")
            null
        }
    }
}