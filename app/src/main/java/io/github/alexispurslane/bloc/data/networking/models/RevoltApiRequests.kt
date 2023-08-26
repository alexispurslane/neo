package io.github.alexispurslane.bloc.data.networking.models

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

// IMPORTANT: Nullable fields on requests must be named
// exaclty how they must appear in the JSON, because Jackson
// ignores JsonProperties on nullable fields for some reason
data class LoginRequest(
    @JsonProperty("email") val email: String,
    @JsonProperty("password") val password: String,
    @JsonProperty("friendly_name") val friendly_name: String? = null
)

data class MFALoginRequest(
    @JsonProperty("mfa_ticket") val mfa_ticket: String,
    @JsonProperty("mfa_response") val mfa_respose: MFAResponse,
    @JsonProperty("friendly_name") val friendly_name: String?
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MFAResponse(
    @JsonProperty("password") val password: String? = null,
    @JsonProperty("totp_code") val totp_code: String? = null,
    @JsonProperty("recovery_code") val recovery_code: String? = null
)
