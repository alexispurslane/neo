package io.github.alexispurslane.bloc.data.network.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RevoltWebSocketRequest.Authenticate::class, name = "Authenticate"),
    JsonSubTypes.Type(value = RevoltWebSocketRequest.BeginTyping::class, name = "BeginTyping"),
    JsonSubTypes.Type(value = RevoltWebSocketRequest.EndTyping::class, name = "EndTyping"),
    JsonSubTypes.Type(value = RevoltWebSocketRequest.Ping::class, name = "Ping"),
)
sealed class RevoltWebSocketRequest private constructor() {
    data class Authenticate(
        @get:JsonProperty("type") val type: String = "Authenticate",
        @get:JsonProperty("token") val sessionToken: String
    ) : RevoltWebSocketRequest()
    data class BeginTyping(
        @get:JsonProperty("type") val type: String = "BeginTyping",
        @get:JsonProperty("channel") val channelId: String
    )  : RevoltWebSocketRequest()

    data class EndTyping(
        @get:JsonProperty("type") val type: String = "EndTyping",
        @get:JsonProperty("channel") val channelId: String
    )  : RevoltWebSocketRequest()

    data class Ping(
        @get:JsonProperty("type") val type: String = "Ping",
        @get:JsonProperty("data") var timestamp: Long = 0
    ) : RevoltWebSocketRequest()
}

enum class RevoltErrorId(@JsonValue val errorId: String) {
    // Uncategorized issue
    LABEL_ME("LabelMe"),
    // The server ran into an issue
    INTERNAL_ERROR("InternalError"),
    // Authentication details are incorrect
    INVALID_SESSION("InvalidSession"),
    // user has not chosen a username
    ONBOARDING_NOT_FINISHED("OnboardingNotFinished"),
    // this connection is already authenticated
    ALREADY_AUTHENTICATED("AlreadyAuthenticated")
}
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RevoltWebSocketResponse.Error::class, name = "Error"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.Authenticated::class, name = "Authenticated"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.Bulk::class, name = "Bulk"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.Pong::class, name = "Pong"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.Ready::class, name = "Ready"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.Message::class, name = "Message"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.MessageUpdate::class, name = "MessageUpdate"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.MessageAppend::class, name = "MessageAppend"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.MessageDelete::class, name = "MessageDelete"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.MessageReact::class, name = "MessageReact"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.MessageUnreact::class, name = "MessageUnreact"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.MessageRemoveReaction::class, name = "MessageRemoveReaction"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ChannelCreate::class, name = "ChannelCreate"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ChannelUpdate::class, name = "ChannelUpdate"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ChannelDelete::class, name = "ChannelDelete"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ChannelGroupJoin::class, name = "ChannelGroupJoin"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ChannelGroupLeave::class, name = "ChannelGroupLeave"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ChannelStartTyping::class, name = "ChannelStartTyping"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ChannelStopTyping::class, name = "ChannelStopTyping"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ChannelAcknowledge::class, name = "ChannelAck"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ServerCreate::class, name = "ServerCreate"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ServerUpdate::class, name = "ServerUpdate"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ServerDelete::class, name = "ServerDelete"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ServerMemberUpdate::class, name = "ServerMemberUpdate"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ServerMemberJoin::class, name = "ServerMemberJoin"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ServerMemberLeave::class, name = "ServerMemberLeave"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ServerRoleUpdate::class, name = "ServerRoleUpdate"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.ServerRoleDelete::class, name = "ServerRoleDelete"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.UserUpdate::class, name = "UserUpdate"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.UserRelationship::class, name = "UserRelationship"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.UserPlatformWipe::class, name = "UserPlatformWipe"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.EmojiCreate::class, name = "EmojiCreate"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.EmojiDelete::class, name = "EmojiDelete"),
    JsonSubTypes.Type(value = RevoltWebSocketResponse.Auth::class, name = "Auth"),
)
sealed class RevoltWebSocketResponse constructor() {
    data class Error(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("error") val errorId: RevoltErrorId
    ) : RevoltWebSocketResponse()
    data class Authenticated(
        @param:JsonProperty("type") val type: String
    ) : RevoltWebSocketResponse()
    data class Bulk(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("v") val events: List<RevoltWebSocketResponse>
    ) : RevoltWebSocketResponse()
    data class Pong(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("data") val timestamp: Long = 0
    ) : RevoltWebSocketResponse()
    data class Ready(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("users") val users: List<RevoltUser>,
        @param:JsonProperty("servers") val servers: List<RevoltServer>,
        @param:JsonProperty("channels") val channels: List<RevoltChannel>,
        @param:JsonProperty("emojis") val emojis: List<Emoji>,
        @param:JsonProperty("members") val members: JsonNode
    ) : RevoltWebSocketResponse()
    data class Message(
        @param:JsonProperty("type") val type: String,
        @JsonUnwrapped
        val message: RevoltMessage
    ) : RevoltWebSocketResponse()
    data class MessageUpdate(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val messageId: String,
        @param:JsonProperty("channel") val channelId: String,
        @param:JsonProperty("data") val data: RevoltMessage,
    ) : RevoltWebSocketResponse()
    data class MessageAppend(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val messageId: String,
        @param:JsonProperty("channel") val channelId: String,
        @param:JsonProperty("append") val append: JsonNode,
    ) : RevoltWebSocketResponse()
    data class MessageDelete(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val messageId: String,
        @param:JsonProperty("channel") val channelId: String,
    ) : RevoltWebSocketResponse()
    data class MessageReact(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val messageId: String,
        @param:JsonProperty("channel") val channelId: String,
        @param:JsonProperty("user_id") val userId: String,
        @param:JsonProperty("emoji_id") val emojiId: String,
    ) : RevoltWebSocketResponse()
    data class MessageUnreact(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val messageId: String,
        @param:JsonProperty("channel") val channelId: String,
        @param:JsonProperty("user_id") val userId: String,
        @param:JsonProperty("emoji_id") val emojiId: String,
    ) : RevoltWebSocketResponse()
    data class MessageRemoveReaction(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val messageId: String,
        @param:JsonProperty("channel") val channelId: String,
        @param:JsonProperty("emoji_id") val emojiId: String,
    ) : RevoltWebSocketResponse()
    data class ChannelCreate(
        @param:JsonProperty("type") val type: String,
        @JsonUnwrapped
        val message: RevoltChannel
    ) : RevoltWebSocketResponse()
    data class ChannelUpdate(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val channelId: String,
        @param:JsonProperty("data") val data: RevoltChannel,
        @param:JsonProperty("clear") val clear: List<String>,
    ) : RevoltWebSocketResponse()
    data class ChannelDelete(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val channelId: String,
    ) : RevoltWebSocketResponse()
    data class ChannelGroupJoin(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val channelId: String,
        @param:JsonProperty("user") val userId: String,
    ) : RevoltWebSocketResponse()
    data class ChannelGroupLeave(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val channelId: String,
        @param:JsonProperty("user") val userId: String,
    ) : RevoltWebSocketResponse()
    data class ChannelStartTyping(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val channelId: String,
        @param:JsonProperty("user") val userId: String,
    ) : RevoltWebSocketResponse()
    data class ChannelStopTyping(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val channelId: String,
        @param:JsonProperty("user") val userId: String,
    ) : RevoltWebSocketResponse()
    data class ChannelAcknowledge(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val channelId: String,
        @param:JsonProperty("user") val userId: String,
        @param:JsonProperty("message_id") val messageId: String,
    ) : RevoltWebSocketResponse()
    data class ServerCreate(
        @param:JsonProperty("type") val type: String,
        @JsonUnwrapped
        val server: RevoltServer
    ) : RevoltWebSocketResponse()
    data class ServerUpdate(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val serverId: String,
        @param:JsonProperty("data") val data: RevoltServer,
        @param:JsonProperty("clear") val clear: List<String>,
    ) : RevoltWebSocketResponse()
    data class ServerDelete(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val serverId: String,
    ) : RevoltWebSocketResponse()
    data class ServerMemberUpdate(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val ids: Map<String, String>,
        @param:JsonProperty("data") val data: JsonNode,
        @param:JsonProperty("clear") val clear: List<String>,
    ) : RevoltWebSocketResponse()
    data class ServerMemberJoin(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val serverId: String,
        @param:JsonProperty("user") val userId: String,
    ) : RevoltWebSocketResponse()
    data class ServerMemberLeave(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val serverId: String,
        @param:JsonProperty("user") val userId: String,
    ) : RevoltWebSocketResponse()
    data class ServerRoleUpdate(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val serverId: String,
        @param:JsonProperty("role_id") val roleId: String,
        @param:JsonProperty("data") val data: Role,
        @param:JsonProperty("clear") val clear: List<String>,
    ) : RevoltWebSocketResponse()
    data class ServerRoleDelete(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val serverId: String,
        @param:JsonProperty("role_id") val roleId: String,
    ) : RevoltWebSocketResponse()
    data class UserUpdate(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val userId: String,
        @param:JsonProperty("data") val data: RevoltUser,
        @param:JsonProperty("clear") val clear: List<String>,
    ) : RevoltWebSocketResponse()
    data class UserRelationship(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val yourUserId: String,
        @param:JsonProperty("user") val user: RevoltUser,
        @param:JsonProperty("status") val status: RelationshipStatus,
    ) : RevoltWebSocketResponse()
    data class UserPlatformWipe(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("user_id") val userId: String,
        @param:JsonProperty("flags") val flags: String,
    ) : RevoltWebSocketResponse()
    data class EmojiCreate(
        @param:JsonProperty("type") val type: String,
        @JsonUnwrapped
        val emoji: Emoji
    ) : RevoltWebSocketResponse()
    data class EmojiDelete(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("id") val emojiId: String,
    ) : RevoltWebSocketResponse()
    data class Auth(
        @param:JsonProperty("type") val type: String,
        @param:JsonProperty("event_type") val eventType: String,
        @param:JsonProperty("user_id") val userId: String,
        @param:JsonProperty("session_id") val sessionId: String,
        @param:JsonProperty("exclude_session_id") val excludeSessionId: String,
    ) : RevoltWebSocketResponse()
}