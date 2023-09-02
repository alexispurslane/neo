package io.github.alexispurslane.bloc.data.network.models

import androidx.annotation.Keep
import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode


data class BuildResponse(
    @JsonProperty("commit_sha") val commitSha: String,
    @JsonProperty("commit_timestamp") val commitTimestamp: String,
    @JsonProperty("semver") val semanticVersion: String,
    @JsonProperty("origin_url") val originUrl: String,
    @JsonProperty("timestamp") val timestamp: String,
)


data class QueryNodeResponse(
    @JsonProperty("revolt") val revolt: String,
    @JsonProperty("ws") val ws: String,
    @JsonProperty("app") val app: String,
    @JsonProperty("vapid") val vapid: String,
    @JsonProperty("features") val features: JsonNode,
    @JsonProperty("build") val build: BuildResponse
)


data class WebPushSubscriptionResponse(
    @JsonProperty("endpoint") val endpoint: String,
    @JsonProperty("p256dh") val p256dh: String,
    @JsonProperty("auth") val auth: String,
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "result",
    visible = true
)
@JsonSubTypes(
    Type(value = LoginResponse.Success::class, name = "Success"),
    Type(value = LoginResponse.Disabled::class, name = "Disabled"),
    Type(value = LoginResponse.MFA::class, name = "MFA"),
)
sealed class LoginResponse private constructor() {
    @Keep
    data class Success(
        @param:JsonProperty("result") val result: String = "Success",
        @param:JsonProperty("_id") val id: String,
        @param:JsonProperty("user_id") val userId: String,
        @param:JsonProperty("token") val sessionToken: String,
        @param:JsonProperty("name") val displayName: String,
        @param:JsonProperty("subscription") val webPushSubscription: WebPushSubscriptionResponse? = null
    ) : LoginResponse()

    @Keep
    data class Disabled(
        @param:JsonProperty("result") val result: String = "Disabled",
        @param:JsonProperty("user_id") val userId: String,
    ) : LoginResponse()

    @Keep
    data class MFA(
        @param:JsonProperty("result") val result: String = "MFA",
        @param:JsonProperty("ticket") val ticket: String,
        @param:JsonProperty("allowed_methods") val allowedMethods: List<String>,
    ) : LoginResponse()
}

enum class RelationshipStatus(@JsonValue val status: String) {
    NONE("None"),
    USER("User"),
    FRIEND("Friend"),
    OUTGOING("Outgoing"),
    INCOMING("Incoming"),
    BLOCKED("Blocked"),
    BLOCKED_OTHER("BlockedOther"),
}

enum class Presence(@JsonValue val presence: String) {
    ONLINE("Online"),
    IDLE("Idle"),
    FOCUS("Focus"),
    BUSY("Busy"),
    Invisible("Invisible"),
}


data class RevoltUser(
    @param:JsonAlias("id")
    @param:JsonProperty("_id") val userId: String,
    @param:JsonProperty("username") val userName: String,
    @param:JsonProperty("discriminator") val discriminator: String,
    @param:JsonProperty("display_name") val displayName: String?,
    @param:JsonProperty("avatar") val avatar: AutumnFile?,
    @param:JsonProperty("relations") val relations: List<UserRelation>?,
    @param:JsonProperty("badges") val badges: Int?,
    @param:JsonProperty("status") val status: UserStatus?,
    @param:JsonProperty("profile") var profile: UserProfile?,
    @param:JsonProperty("flags") val flags: Int?,
    @param:JsonProperty("privileged") val privileged: Boolean?,
    @param:JsonProperty("bot") val botInformation: BotInformation?,
    @param:JsonProperty("relationship") val relationship: String?,
    @param:JsonProperty("online") val online: Boolean?,
)


data class AutumnFile(
    @param:JsonProperty("_id") val fileId: String,
    @param:JsonProperty("tag") val fileTag: String,
    @param:JsonProperty("filename") val fileName: String,
    @param:JsonProperty("metadata") val metadata: RevoltFileMetadata,
    @param:JsonProperty("content_type") val contentType: String,
    @param:JsonProperty("size") val byteSize: Int,
    @param:JsonProperty("deleted") val wasDeleted: Boolean?,
    @param:JsonProperty("reported") val wasReported: Boolean?,
    @param:JsonProperty("message_id") val messageId: String?,
    @param:JsonProperty("user_id") val userId: String?,
    @param:JsonProperty("server_id") val serverId: String?,
    @param:JsonProperty("object_id") val objectId: String?,
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    Type(value = RevoltFileMetadata.File::class, name = "File"),
    Type(value = RevoltFileMetadata.Text::class, name = "Text"),
    Type(value = RevoltFileMetadata.Image::class, name = "Image"),
    Type(value = RevoltFileMetadata.Video::class, name = "Video"),
    Type(value = RevoltFileMetadata.Audio::class, name = "Audio"),
)
sealed class RevoltFileMetadata private constructor() {
    @Keep
    data class File(
        @param:JsonProperty("type") val type: String,
    ) : RevoltFileMetadata()

    @Keep
    data class Text(
        @param:JsonProperty("type") val type: String,
    ) : RevoltFileMetadata()

    @Keep
    data class Audio(
        @param:JsonProperty("type") val type: String,
    ) : RevoltFileMetadata()

    @Keep
    data class Image(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("width") val width: Int,
        @param:JsonProperty("height") val height: Int,
    ) : RevoltFileMetadata()

    @Keep
    data class Video(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("width") val width: Int,
        @param:JsonProperty("height") val height: Int,
    ) : RevoltFileMetadata()
}


data class UserRelation(
    @param:JsonProperty("_id") val userId: String,
    @param:JsonProperty("status") val status: RelationshipStatus
)


data class UserStatus(
    @param:JsonProperty("text") val customStatus: String?,
    @param:JsonProperty("presence") val presence: Presence?
)


data class UserProfile(
    @param:JsonProperty("content") val content: String?,
    @param:JsonProperty("background") val background: AutumnFile?,
)


data class BotInformation(
    @param:JsonProperty("owner") val ownerId: String
)


data class RevoltServer(
    @param:JsonProperty("_id") val serverId: String,
    @param:JsonProperty("owner") val ownerId: String,
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("description") val description: String?,
    @param:JsonProperty("channels") val channelsIds: List<String>,
    @param:JsonProperty("categories") var categories: List<ServerCategory>,
    @param:JsonProperty("system_messages") var systemMessagesConfig: SystemMessagesConfig?,
    @param:JsonProperty("roles") var roles: Map<String, Role>?,
    @param:JsonProperty("default_permissions") var defaultPermissions: Long,
    @param:JsonProperty("icon") var icon: AutumnFile?,
    @param:JsonProperty("banner") var banner: AutumnFile?,
    @param:JsonProperty("flags") var flags: Int?,
    @param:JsonProperty("nsfw") var nsfw: Boolean?,
    @param:JsonProperty("analytics") var analytics: Boolean?,
    @param:JsonProperty("discoverable") var discoverable: Boolean?,
)


data class ServerCategory(
    @param:JsonProperty("id") val categoryId: String,
    @param:JsonProperty("title") val title: String,
    @param:JsonProperty("channels") val channelIds: List<String>,
)


data class SystemMessagesConfig(
    @param:JsonProperty("user_joined") val userJoinedChannelId: String?,
    @param:JsonProperty("user_left") val userLeftChannelId: String?,
    @param:JsonProperty("user_kicked") val userKickedChannelId: String?,
    @param:JsonProperty("user_banned") val userBannedChannelId: String?,
)


data class Role(
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("permissions") val permissions: Permissions,
    @param:JsonProperty("colour") val color: String?,
    @param:JsonProperty("hoist") val hoist: Boolean?,
    @param:JsonProperty("rank") val rank: Int = 0,
)


data class Permissions(
    @param:JsonProperty("a") val allow: Long,
    @param:JsonProperty("d") val disallow: Long,
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "channel_type",
    visible = true
)
@JsonSubTypes(
    Type(value = RevoltChannel.SavedMessages::class, name = "SavedMessages"),
    Type(value = RevoltChannel.DirectMessage::class, name = "DirectMessage"),
    Type(value = RevoltChannel.Group::class, name = "Group"),
    Type(value = RevoltChannel.TextChannel::class, name = "TextChannel"),
    Type(value = RevoltChannel.VoiceChannel::class, name = "VoiceChannel"),
)
sealed class RevoltChannel private constructor() {
    @Keep
    data class SavedMessages(
        @param:JsonProperty("channel_type") val channelType: String,
        @param:JsonProperty("_id") val channelId: String,
        @param:JsonProperty("user") val userId: String
    ) : RevoltChannel()

    @Keep
    data class DirectMessage(
        @param:JsonProperty("channel_type") val channelType: String,
        @param:JsonProperty("_id") val channelId: String,
        @param:JsonProperty("active") val isActive: Boolean,
        @param:JsonProperty("recipients") val participantUserIds: List<String>,
        @param:JsonProperty("last_message_id") val lastMessageId: String?,
    ) : RevoltChannel()

    @Keep
    data class Group(
        @param:JsonProperty("channel_type") val channelType: String,
        @param:JsonProperty("_id") val channelId: String,
        @param:JsonProperty("name") val name: String,
        @param:JsonProperty("owner") val owner: String,
        @param:JsonProperty("description") val description: String?,
        @param:JsonProperty("recipients") val participantUserIds: List<String>,
        @param:JsonProperty("icon") val icon: AutumnFile?,
        @param:JsonProperty("last_message_id") val lastMessageId: String?,
        @param:JsonProperty("permissions") val permissions: Long?,
        @param:JsonProperty("nsfw") val nsfw: Boolean?,
    ) : RevoltChannel()

    @Keep
    data class TextChannel(
        @param:JsonProperty("channel_type") val channelType: String,
        @param:JsonProperty("_id") val channelId: String,
        @param:JsonProperty("server") val serverId: String,
        @param:JsonProperty("name") val name: String,
        @param:JsonProperty("description") val description: String?,
        @param:JsonProperty("icon") val icon: AutumnFile?,
        @param:JsonProperty("last_message_id") val lastMessageId: String?,
        @param:JsonProperty("default_permissions") val defaultPermissions: Long?,
        @param:JsonProperty("role_permissions") val rolePermissions: Map<String, Permissions>?,
        @param:JsonProperty("nsfw") val nsfw: Boolean?,
    ) : RevoltChannel()

    @Keep
    data class VoiceChannel(
        @param:JsonProperty("channel_type") val channelType: String,
        @param:JsonProperty("_id") val channelId: String,
        @param:JsonProperty("server") val serverId: String,
        @param:JsonProperty("name") val name: String,
        @param:JsonProperty("description") val description: String?,
        @param:JsonProperty("icon") val icon: AutumnFile?,
        @param:JsonProperty("default_permissions") val defaultPermissions: Long?,
        @param:JsonProperty("role_permissions") val rolePermissions: Map<String, Permissions>?,
        @param:JsonProperty("nsfw") val nsfw: Boolean?,
    ) : RevoltChannel()
}


data class Emoji(
    @param:JsonProperty("_id") val emojiId: String,
    @param:JsonProperty("parent") val parent: EmojiParent,
    @param:JsonProperty("creator_id") val creatorId: String,
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("animated") val animated: Boolean?,
    @param:JsonProperty("nsfw") val nsfw: Boolean?,
)


data class EmojiParent(
    @param:JsonProperty("type") val type: String,
    @param:JsonProperty("id") val serverId: String?
)


data class RevoltMessage(
    @param:JsonProperty("_id") val messageId: String,
    @param:JsonProperty("nonce") val nonce: String?,
    @param:JsonProperty("channel") val channelId: String,
    @param:JsonProperty("author") val authorId: String,
    @param:JsonProperty("webhook") val webhook: Webhook?,
    @param:JsonProperty("content") val content: String?,
    @param:JsonProperty("system") val systemEventMessage: SystemEventMessage?,
    @param:JsonProperty("attachments") val attachments: List<AutumnFile>?,
    @param:JsonProperty("edited") val edited: String?,
    @param:JsonProperty("embeds") val embeds: List<JsonNode>?,
    @param:JsonProperty("mentions") val mentionedIds: List<String>?,
    @param:JsonProperty("replies") val replyIds: List<String>?,
    @param:JsonProperty("reactions") val reactions: Map<String, String>?,
    @param:JsonProperty("interactions") val interactions: InteractionsGuide?,
    @param:JsonProperty("masquerade") val masquerade: Masquerade?,
)

data class RevoltMessageSent(
    @param:JsonProperty("content") val content: String?,
    @param:JsonProperty("attachments") val attachments: List<AutumnFile>?,
    @param:JsonProperty("replies") val replyIds: List<String>?,
    @param:JsonProperty("embeds") val embeds: List<JsonNode>?,
    @param:JsonProperty("masquerade") val masquerade: Masquerade?,
    @param:JsonProperty("interactions") val interactions: InteractionsGuide?,
)


data class Masquerade(
    @param:JsonProperty("name") val name: String?,
    @param:JsonProperty("avatar") val avatarUrl: String?,
    @param:JsonProperty("colour") val color: String?,
)


data class InteractionsGuide(
    @param:JsonProperty("reactions") val reactions: List<String>?,
    @param:JsonProperty("restrict_reactions") val restrictReactions: Boolean?,
)

// TODO: Turn this into a discriminated union like the other stuff for proper type-safety

data class SystemEventMessage(
    @param:JsonProperty("type") val type: String,
    @param:JsonProperty("content") val content: String?,
    @param:JsonProperty("name") val name: String?,
    @param:JsonProperty("id") val id: String?,
    @param:JsonProperty("from") val from: String?,
    @param:JsonProperty("by") val by: String?,
)


data class Webhook(
    @param:JsonProperty("name") val name: String,
    @param:JsonProperty("avatar") val avatar: String?
)


data class RevoltMembersResponse(
    @param:JsonProperty("members") val members: List<RevoltServerMember>,
    @param:JsonProperty("users") val users: List<RevoltUser>
)


data class RevoltServerMember(
    @param:JsonProperty("_id") val compositePrimaryId: RevoltCompositeId,
    @param:JsonProperty("joined_at") val joinedAt: String,
    @param:JsonProperty("nickname") val nickname: String?,
    @param:JsonProperty("avatar") val avatar: AutumnFile?,
    @param:JsonProperty("roles") val roles: List<String>?,
    @param:JsonProperty("timeout") val timeout: String?,
)


data class RevoltCompositeId(
    @param:JsonProperty("server") val server: String,
    @param:JsonProperty("user") val user: String,
)