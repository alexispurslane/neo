package io.github.alexispurslane.neo.ui.composables.misc

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.core.content.FileProvider
import io.github.alexispurslane.neo.R
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.media
import net.folivo.trixnity.utils.toByteArray

fun ByteArray.asImageBitmap(): ImageBitmap =
    BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()

class MatrixImageFileProvider : FileProvider(R.xml.file_paths)

@Composable
fun MatrixImage(
    modifier: Modifier = Modifier,
    client: MatrixClient,
    mxcUri: String,
    shouldFill: Boolean = true
) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(size) {
        if (size != IntSize.Zero) {
            val bytes = client.media
                .getThumbnail(mxcUri, size.width.toLong(), size.height.toLong())
                .getOrNull()
                ?.toByteArray()
            bitmap = try {
                bytes?.asImageBitmap()
            } catch (e: Exception) {
                Log.e("MatrixImage", "Error with image bytes: $mxcUri")
                null
            }
        }
    }

    Box(
        modifier = modifier.onGloballyPositioned { size = it.size }
    ) {
        bitmap?.let {
            Image(
                modifier = if (shouldFill) Modifier.fillMaxSize() else Modifier.fillMaxHeight(),
                bitmap = it,
                contentDescription = null,
                contentScale = if (shouldFill) ContentScale.Crop else ContentScale.Fit
            )
        }
    }
}
