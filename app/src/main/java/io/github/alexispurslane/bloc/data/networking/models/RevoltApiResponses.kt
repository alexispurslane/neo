package io.github.alexispurslane.bloc.data.networking.models

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
    data class Success(
        @param:JsonProperty("result") val result: String = "Success",
        @param:JsonProperty("_id") val id: String,
        @param:JsonProperty("user_id") val userId: String,
        @param:JsonProperty("token") val sessionToken: String,
        @param:JsonProperty("name") val displayName: String,
        @param:JsonProperty("subscription") val webPushSubscription: WebPushSubscriptionResponse? = null
    ) : LoginResponse()

    data class Disabled(
        @param:JsonProperty("result") val result: String = "Disabled",
        @param:JsonProperty("user_id") val userId: String,
    ) : LoginResponse()

    data class MFA(
        @param:JsonProperty("result") val result: String = "MFA",
        @param:JsonProperty("ticket") val ticket: String,
        @param:JsonProperty("allowed_methods") val allowedMethods: List<String>,
    ) : LoginResponse()
}