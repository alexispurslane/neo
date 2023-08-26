package io.github.alexispurslane.bloc.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.networking.RetrofitInstance
import io.github.alexispurslane.bloc.data.networking.models.LoginResponse
import io.github.alexispurslane.bloc.data.networking.models.LoginRequest
import io.github.alexispurslane.bloc.data.networking.models.QueryNodeResponse
import io.github.alexispurslane.bloc.data.networking.models.WebPushSubscriptionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class UserSession(
    val instanceApiUrl: String? = null,
    val emailAddress: String? = null,
    val sessionId: String? = null,
    val userId: String? = null,
    val sessionToken: String? = null,
    val displayName: String? = null
)

@Singleton
class RevoltAccountsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    var userSessionFlow: Flow<UserSession> = dataStore.data.catch {
        if (it is IOException)
            emit(emptyPreferences())
        else
            throw it
    }.map {
        UserSession(
            instanceApiUrl = it[PreferenceKeys.INSTANCE_API_URL],
            emailAddress = it[PreferenceKeys.EMAIL],
            sessionId = it[PreferenceKeys.SESSION_ID],
            userId = it[PreferenceKeys.USER_ID],
            sessionToken = it[PreferenceKeys.SESSION_TOKEN],
            displayName = it[PreferenceKeys.DISPLAY_NAME]
        )
    }
    var webPushSubscription: MutableStateFlow<WebPushSubscriptionResponse?> =
        MutableStateFlow(null)

    suspend fun queryNode(baseUrl: String): Response<QueryNodeResponse> {
        return RetrofitInstance.revoltApiService(baseUrl).queryNode(baseUrl)
    }

    suspend fun login(baseUrl: String, emailAddress: String, loginRequest: LoginRequest): Either<LoginResponse, String> {
        return try {
            val res = when (loginRequest) {
                is LoginRequest.Basic -> RetrofitInstance.revoltApiService(baseUrl)
                    .login(loginRequest)
                is LoginRequest.MFA -> RetrofitInstance.revoltApiService(baseUrl)
                    .login(loginRequest)
            }

            if (res.isSuccessful) {
                val loginResponse = res.body()!!
                saveLoginInfo(
                    baseUrl,
                    emailAddress,
                    loginResponse
                )
                Either.Success(loginResponse)
            } else {
                val errorBody = res.errorBody()?.string()
                if (errorBody != null) {
                    val jsonObject = JSONObject(errorBody.trim())
                    Either.Error("Uh oh! ${res.message()}:The server error was '${jsonObject.getString("type")}'")
                } else {
                    Either.Error("Uh oh! The server returned an error:${res.message()}")
                }
            }
        } catch (e: Exception) {
            Either.Error("Uh oh! Could not send login request:${e.message!!}")
        }
    }

    private suspend fun saveLoginInfo(api: String, email: String, loginResponse: LoginResponse) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.INSTANCE_API_URL] = api
            preferences[PreferenceKeys.EMAIL] = email
            Log.d("DATA STORE", "Saved: $api, $email")

            if (loginResponse is LoginResponse.Success) {
                webPushSubscription.update {
                    loginResponse.webPushSubscription
                }
                preferences[PreferenceKeys.SESSION_ID] = loginResponse.id
                preferences[PreferenceKeys.USER_ID] = loginResponse.userId
                preferences[PreferenceKeys.SESSION_TOKEN] = loginResponse.sessionToken
                preferences[PreferenceKeys.DISPLAY_NAME] = loginResponse.displayName
            }
        }
    }
}

private object PreferenceKeys {
    val EMAIL = stringPreferencesKey("email")
    val INSTANCE_API_URL = stringPreferencesKey("instance_api_url")
    val SESSION_ID = stringPreferencesKey("session_id")
    val USER_ID = stringPreferencesKey("user_id")
    val SESSION_TOKEN = stringPreferencesKey("session_token")
    val DISPLAY_NAME = stringPreferencesKey("display_name")
}
