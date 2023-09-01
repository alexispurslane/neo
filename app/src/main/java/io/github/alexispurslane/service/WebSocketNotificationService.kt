package io.github.alexispurslane.service

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import dagger.hilt.android.AndroidEntryPoint
import io.github.alexispurslane.bloc.MainActivity
import io.github.alexispurslane.bloc.R
import io.github.alexispurslane.bloc.SERVER_CHANNEL_ID
import io.github.alexispurslane.bloc.SERVICE_CHANNEL_ID
import io.github.alexispurslane.bloc.data.PreferenceKeys
import io.github.alexispurslane.bloc.data.network.RevoltApiModule
import io.github.alexispurslane.bloc.data.network.RevoltWebSocketModule
import io.github.alexispurslane.bloc.data.network.models.RevoltChannel
import io.github.alexispurslane.bloc.data.network.models.RevoltMessage
import io.github.alexispurslane.bloc.data.network.models.RevoltWebSocketResponse
import io.github.alexispurslane.bloc.ui.theme.EngineeringOrange
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.net.ssl.HttpsURLConnection

enum class Actions {
    START,
    STOP
}

@AndroidEntryPoint
class WebSocketNotificationService : Service() {

    private var listenJob: Job? = null
    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    private val notificationId = AtomicInteger(0)

    private fun getNotificationId() = notificationId.incrementAndGet()

    override fun onBind(intent: Intent?): IBinder? {
        Log.w(
            "WEBSOCKET NOTIF SERVICE",
            "Some component attempted to bind with WebSocketNotificationService"
        )
        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        Log.d(
            "WEBSOCKET NOTIF SERVICE",
            "onStartCommand executed with start id $startId"
        )
        if (intent != null) {
            when (intent.action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartServiceIntent = Intent(
            applicationContext,
            WebSocketNotificationService::class.java
        ).apply {
            setPackage(packageName)
        }
        val restartServicePendingIntent =
            PendingIntent.getService(
                this,
                1,
                restartServiceIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

        val alarmService =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        )
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("WEBSOCKET NOTIF SERVICE", "Service created")
        val notification = createServiceNotification()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("WEBSOCKET NOTIF SERVICE", "Service destroyed")
        Toast.makeText(
            this,
            "Bloc notification service destroyed",
            Toast.LENGTH_SHORT
        ).show()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun startService() {
        if (isServiceStarted) return
        Log.d("WEBSOCKET NOTIF SERVICE", "Starting foreground service task")
        Toast.makeText(
            this,
            "Bloc notification service starting task",
            Toast.LENGTH_SHORT
        ).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "WebsocketNotificationService::lock"
                ).apply {
                    acquire(10 * 60 * 1000L /*10 minutes*/)
                }
            }

        val context = this
        listenJob = GlobalScope.launch(Dispatchers.IO) {
            dataStore.data.catch {
                if (it is IOException)
                    emit(emptyPreferences())
                else throw it
            }.collect { preferences ->
                val apiUrl = preferences[PreferenceKeys.INSTANCE_API_URL]
                val sessionToken = preferences[PreferenceKeys.SESSION_TOKEN]
                val websocketUrl = preferences[PreferenceKeys.WEBSOCKETS_URL]
                if (apiUrl != null && sessionToken != null && websocketUrl != null) {
                    RevoltApiModule.setBaseUrl(apiUrl)
                    RevoltWebSocketModule.setWebSocketUrlAndToken(
                        websocketUrl,
                        sessionToken
                    )

                    RevoltWebSocketModule.eventFlow.collect { event ->
                        when (event) {
                            is RevoltWebSocketResponse.Message -> {
                                Log.d(
                                    "WEBSOCKET NOTIF SERVICE",
                                    "Received message: ${event.message}"
                                )
                                with(NotificationManagerCompat.from(context)) {
                                    if (ActivityCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        val notification =
                                            createMessageNotification(
                                                context,
                                                sessionToken,
                                                event.message
                                            )
                                        notify(
                                            getNotificationId(),
                                            notification
                                        )
                                    }
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun stopService() {
        Log.d("WEBSOCKET NOTIF SERVICE", "Stopping foreground service")
        Toast.makeText(
            this,
            "Bloc notification service stopping",
            Toast.LENGTH_SHORT
        ).show()

        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            listenJob?.cancel()
            stopSelf()
        } catch (e: Exception) {
            Log.e(
                "WEBSOCKET NOTIF SERVICE",
                "Service stopped without being started: ${e.message}"
            )
        }

        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }

    private val people: MutableMap<String, Person> = mutableMapOf()
    private suspend fun createMessageNotification(
        context: Context,
        sessionToken: String,
        message: RevoltMessage
    ): Notification = coroutineScope {
        val pendingIntent = Intent(
            Intent.ACTION_VIEW,
            "bloc://channel//${message.channelId}".toUri(),
            context,
            MainActivity::class.java
        ).let {
            PendingIntent.getActivity(
                context,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        val authorPerson = people.getOrPut(message.authorId) {
            RevoltApiModule.service().fetchUser(sessionToken, message.authorId)
                .let {
                    if (it.isSuccessful) {
                        val author = it.body()!!
                        var personBuilder = Person.Builder()
                            .setName("@${author.displayName}")
                        author.avatar?.let { file ->
                            RevoltApiModule.getResourceUrl(file)?.let { uri ->
                                try {
                                    val url = URL(uri)
                                    with(url.openConnection() as HttpsURLConnection) {
                                        requestMethod = "GET"
                                        doInput = true

                                        val bitmap =
                                            BitmapFactory.decodeStream(
                                                inputStream
                                            )

                                        personBuilder = personBuilder
                                            .setIcon(
                                                IconCompat.createWithBitmap(
                                                    bitmap
                                                )
                                            )
                                    }
                                } catch (e: Exception) {
                                    Log.w(
                                        "WEBSOCKET NOTIF SERVICE",
                                        "Got error pulling user avatar: ${e.message}"
                                    )
                                }
                            }
                        }
                        personBuilder.build()
                    } else {
                        Person.Builder().build()
                    }
                }
        }
        val channelName = RevoltApiModule.service()
            .fetchChannel(sessionToken, message.channelId).let {
            if (it.isSuccessful) {
                when (val channel = it.body()!!) {
                    is RevoltChannel.TextChannel -> {
                        "#${channel.name}"
                    }

                    is RevoltChannel.DirectMessage -> {
                        val usernames = channel.participantUserIds.map {
                            async {
                                val res = RevoltApiModule.service()
                                    .fetchUser(sessionToken, it)
                                if (res.isSuccessful)
                                    res.body()!!.displayName
                                else
                                    null
                            }
                        }.awaitAll().filterNotNull().joinToString(", ")
                        "@$usernames"
                    }

                    else -> {
                        "Unknown Channel"
                    }
                }
            } else {
                "Unknown Channel"
            }
        }
        val builder = NotificationCompat.Builder(context, SERVER_CHANNEL_ID)
            .setSmallIcon(R.drawable.bloc_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(EngineeringOrange.toArgb())
            .setStyle(
                NotificationCompat.MessagingStyle("Me")
                    .setGroupConversation(true)
                    .setConversationTitle(channelName)
                    .addMessage(
                        message.content,
                        System.currentTimeMillis(),
                        authorPerson
                    )
            )

        builder.build()
    }

    private fun createServiceNotification(): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = Notification.Builder(this, SERVICE_CHANNEL_ID)

        return builder.setContentTitle("Bloc Notification Service")
            .setContentText("Listening over WebSockets for new Revolt messages")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.bloc_logo)
            .setOngoing(true)
            .build()
    }

}