package io.github.alexispurslane.bloc.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.alexispurslane.bloc.data.models.User
import io.ktor.http.Url
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.login
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import net.folivo.trixnity.client.user
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.m.Presence
import okio.Path.Companion.toPath
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
    private val platformRepositoryModule = createRealmRepositoriesModule {
        directory(context.cacheDir.absolutePath.toPath().resolve("realm").toString())
    }
    private val platformMediaStore = OkioMediaStore(context.cacheDir.absolutePath.toPath().resolve("media"))
    private var _matrixClientFlow: MutableStateFlow<MatrixClient?> = MutableStateFlow(
        null
    )
    val matrixClientFlow: StateFlow<MatrixClient?> = _matrixClientFlow.asStateFlow()

    val matrixClient: MatrixClient?
        get() = matrixClientFlow.value

    val users: SnapshotStateMap<UserId, User> = mutableStateMapOf()

    private var _userSessionFlow: MutableStateFlow<UserSession?> = MutableStateFlow(null)
    val userSessionFlow: StateFlow<UserSession?> = _userSessionFlow.asStateFlow()


    init {
        GlobalScope.launch(Dispatchers.IO) {
            MatrixClient.fromStore(
                repositoriesModule = platformRepositoryModule,
                mediaStore = platformMediaStore
            ).getOrNull()?.let {
                it.startSync()
                _matrixClientFlow.emit(it)
            }
        }

        GlobalScope.launch(Dispatchers.IO) {
            settingsLocalDataSource.data.catch {
                if (it is IOException)
                    emit(emptyPreferences())
                else
                    throw it
            }.map {
                val instanceApiUrl = it[stringPreferencesKey("instanceApiUrl")]
                val userId = it[stringPreferencesKey("userId")]
                if (instanceApiUrl != null && userId != null) {
                    UserSession(
                        instanceApiUrl = instanceApiUrl,
                        userId = userId,
                        avatarUrl = it[stringPreferencesKey("avatarUrl")]?.let { if (it == "") null else it },
                        displayName = it[stringPreferencesKey("displayName")]?.let { if (it == "") null else it },
                        preferences = it.asMap().map { it.key.name to it.value.toString() }.toMap()
                    )
                } else {
                    null
                }
            }.filterNotNull().collectLatest {
                _userSessionFlow.emit(it)
            }
        }
    }

    suspend fun login(
        baseUrl: String,
        userId: String,
        password: String
    ): Result<MatrixClient> {
        val mc = MatrixClient.login(
            baseUrl = Url(baseUrl),
            identifier = IdentifierType.User(userId),
            repositoriesModule = platformRepositoryModule,
            mediaStore = platformMediaStore,
            password = password
        ).getOrElse { return@login Result.failure(it) }

        Log.d("Account Repository", "Login succeeded")

        mc.startSync()
        Log.d("Account Repository", "Sync started, fetching profile...")

        Log.d("Account Repository", "User ID: $userId")
        val profile = mc.api.user.getProfile(userId(userId, baseUrl)).getOrNull()
        Log.d("Account Repository", "Profile fetched, saving profile...")

        saveLoginInfo(UserSession(
            userId = userId,
            instanceApiUrl = baseUrl,
            avatarUrl = profile?.avatarUrl,
            displayName = profile?.displayName,
        ))

        _matrixClientFlow.emit(mc)

        Log.d("Account Repository", "Login succeeded")
        return Result.success(mc)
    }

    fun userId(userId: String, baseUrl: String? = null): UserId {
        if (baseUrl != null) {
            val domain = baseUrl.replace("https://", "").replace(Regex("/\$"), "") // Domain Expansion: Ultimate Regex
            Log.d("Account Repository", domain)
            return UserId("@$userId:${domain}")
        } else {
            Log.w("Account Repository", "Should probably use fully qualified username")
            return UserId("@$userId")
        }
    }

    suspend fun logout() {
        matrixClient?.apply {
            logout()
            clearCache()
            stop()
        }
        _matrixClientFlow.emit(null)
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

    suspend fun fetchUserInformation(uid: UserId): Result<User> {
        val profile = matrixClient?.api?.user?.getProfile(uid)?.getOrElse { return@fetchUserInformation Result.failure(it) }
        val presence = matrixClient?.api?.user?.getPresence(uid)?.getOrElse { return@fetchUserInformation Result.failure(it) }
        if (profile != null) {
            users[uid] = User(
                userId = uid,
                avatarUrl = profile.avatarUrl,
                displayName = profile.displayName,
                presence = presence?.presence ?: Presence.OFFLINE
            )
            return Result.success(users[uid]!!)
        } else {
            return Result.failure(Exception("Could not fetch user profile information for user ID $uid"))
        }
    }

    suspend fun prefetchUsersForChannel(roomId: RoomId) {
        matrixClient?.user?.getAll(roomId)?.last()?.forEach {
            fetchUserInformation(it.key)
        }
    }
}
