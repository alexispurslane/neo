package io.github.alexispurslane.neo.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.folivo.trixnity.core.model.events.StateEventContent
import net.folivo.trixnity.core.model.events.m.room.ImageInfo

@Serializable
data class ImagePackEventContent(
    @SerialName("images") val images: Map<String, ImageObject>,
    @SerialName("pack") val pack: PackObject? = null,
    override val externalUrl: String? = null
) : StateEventContent

@Serializable
data class PackObject(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("usage") val usage: List<String>? = null,
    @SerialName("attribution") val attribution: String? = null
)

@Serializable
data class ImageObject(
    @SerialName("url") val url: String,
    @SerialName("body") val body: String? = null,
    @SerialName("info") val info: ImageInfo? = null,
    @SerialName("usage") val usage: List<String>? = null
)