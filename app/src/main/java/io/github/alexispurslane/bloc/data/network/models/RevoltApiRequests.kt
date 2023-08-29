package io.github.alexispurslane.bloc.data.network.models

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty


// IMPORTANT: Nullable fields on requests must be named
// exaclty how they must appear in the JSON, because Jackson
// ignores JsonProperties on nullable fields for some reason

sealed class LoginRequest private constructor() {
    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    data class Basic(
        @get:JsonProperty("email") val email: String,
        @get:JsonProperty("password") val password: String,
        @get:JsonProperty("friendly_name") val friendlyName: String? = null
    ) : LoginRequest()

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    data class MFA(
        @get:JsonProperty("mfa_ticket") val mfaTicket: String,
        @get:JsonProperty("mfa_response") val mfaResponse: MFAResponse,
        @get:JsonProperty("friendly_name") val friendlyName: String? = null
    ) : LoginRequest()
}


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class MFAResponse(
    @get:JsonProperty("password") val password: String? = null,
    @get:JsonProperty("totp_code") val totpCode: String? = null,
    @get:JsonProperty("recovery_code") val recoveryCode: String? = null
)

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class MessageRequest(
    @get:JsonProperty("limit") val limit: Int? = null,
    @get:JsonProperty("before") val before: String? = null,
    @get:JsonProperty("after") val after: String? = null,
    @get:JsonProperty("sort") val sort: String? = null,
    @get:JsonProperty("nearby") val nearby: String? = null,
    @get:JsonProperty("include_users") val includeUsers: Boolean? = null
)