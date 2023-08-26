package io.github.alexispurslane.bloc.data.networking.models

import com.fasterxml.jackson.annotation.JsonProperty
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

data class LoginResponse(
    @JsonProperty("result") val result: String,

    // if result == Success
    @JsonProperty("_id") val id: String?,
    @JsonProperty("user_id") val userId: String?, // or if result == Disabled
    @JsonProperty("token") val sessionToken: String?,
    @JsonProperty("name") val displayName: String?,
    @JsonProperty("subscription") val webPushSubscription: WebPushSubscriptionResponse?,

    // if result == MFA
    @JsonProperty("ticket") val ticket: String?,
    @JsonProperty("allowed_methods") val allowedMethods: List<String>,
)