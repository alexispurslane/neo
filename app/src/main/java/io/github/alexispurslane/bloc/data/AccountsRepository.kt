package io.github.alexispurslane.bloc.data

import android.content.Context
import android.provider.Settings.Global
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.alexispurslane.bloc.Either
import io.github.alexispurslane.bloc.data.models.User
import io.ktor.http.Url
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.login
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import net.folivo.trixnity.client.user
import net.folivo.trixnity.client.user.getAccountData
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.server.Search
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence
import okio.Path.Companion.toPath
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class UserSession(
    val instanceApiUrl: String,
    val userId: String,
    val avatarUrl: String?,
    val displayName: String?,
    val preferences: Map<String, String> = emptyMap()
)

@OptIn(DelicateCoroutinesApi::class)
@Singleton
class AccountsRepository @Inject constructor(
    private val settingsLocalDataSource: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) {
    val platformRepositoryModule = createRealmRepositoriesModule {
        directory(context.cacheDir.absolutePath.toPath().resolve("realm").toString())
    }
    val platformMediaStore = OkioMediaStore(context.cacheDir.absolutePath.toPath().resolve("media"))
    var matrixClient: MatrixClient? = null

    val users: SnapshotStateMap<UserId, User> = mutableStateMapOf()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            matrixClient = MatrixClient.fromStore(
                repositoriesModule = platformRepositoryModule,
                mediaStore = platformMediaStore
            ).getOrNull()
            matrixClient?.startSync()
        }
    }

    var userSessionFlow: Flow<UserSession> =
        settingsLocalDataSource.data.catch {
            if (it is IOException)
                emit(emptyPreferences())
            else
                throw it
        }.map {
            UserSession(
                instanceApiUrl = it[stringPreferencesKey("instanceApiUrl")]!!,
                userId = it[stringPreferencesKey("userId")]!!,
                avatarUrl = it[stringPreferencesKey("avatarUrl")]?.let { if (it == "") null else it },
                displayName = it[stringPreferencesKey("displayName")]?.let { if (it == "") null else it },
                preferences = it.asMap().map { it.key.name to it.value.toString() }.toMap()
            )
        }

    suspend fun login(
        baseUrl: String,
        userId: String,
        accessToken: String? = null
    ): Boolean {
        matrixClient = matrixClient
             ?: MatrixClient.login(
                baseUrl = Url(baseUrl),
                identifier = IdentifierType.User(userId),
                repositoriesModule = platformRepositoryModule,
                mediaStore = platformMediaStore,
                token = accessToken
            ).getOrThrow()
        matrixClient?.startSync()
        val profile = matrixClient?.api?.user?.getProfile(UserId(userId))?.getOrNull()
        saveLoginInfo(UserSession(
            userId = userId,
            instanceApiUrl = baseUrl,
            avatarUrl = profile?.avatarUrl,
            displayName = profile?.displayName,
        ))
        return matrixClient != null
    }

    suspend fun logout() {
        matrixClient?.apply {
            logout()
            clearCache()
            clearMediaCache()
            stop()
        }
        matrixClient = null
    }

    private suspend fun saveLoginInfo(
        userSession: UserSession
    ) {
        settingsLocalDataSource.edit {
            it[stringPreferencesKey("instanceApiUrl")] = userSession.instanceApiUrl
            it[stringPreferencesKey("userId")] = userSession.userId
            it[stringPreferencesKey("avatarUrl")] = userSession.avatarUrl ?: ""
            it[stringPreferencesKey("displayName")] = userSession.displayName ?: ""
            userSession.preferences.forEach { entry ->
                it[stringPreferencesKey(entry.key)] = entry.value
            }
        }
    }

    suspend fun savePreferences(preferences: Map<String, String>) {
        settingsLocalDataSource.edit {
            preferences.forEach { entry ->
                it[stringPreferencesKey(entry.key)] = entry.value
            }
        }
    }

    suspend fun clearSession() {
        settingsLocalDataSource.edit {
            it.clear()
        }
    }

    suspend fun fetchUserInformation(userId: UserId): Result<User> {
        val profile = matrixClient?.api?.user?.getPresence(userId)?.getOrNull()
        if (profile != null) {
            users[userId] = User(
                userId = userId,
                avatarUrl = profile.avatarUrl,
                displayName = profile.displayName,
                presence = profile.presence
            )
            return Result.success(users[userId]!!)
        } else {
            return Result.failure(Exception("Could not fetch user profile information for user ID $userId"))
        }
    }

    suspend fun prefetchUsersForChannel(roomId: RoomId) {
        matrixClient?.user?.getAll(roomId)?.last()?.forEach {
            fetchUserInformation(it.key)
        }
    }
}
