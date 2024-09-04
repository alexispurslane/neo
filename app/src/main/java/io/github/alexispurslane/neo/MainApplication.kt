package io.github.alexispurslane.neo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

const val SERVER_CHANNEL_ID: String =
    "io.github.alexispurslane.neo.ServerNotifications"
const val SERVICE_CHANNEL_ID: String =
    "io.github.alexispurslane.neo.BackgroundProcesses"
const val PACKAGE_NAME: String = "io.github.alexispurslane.neo"

@HiltAndroidApp
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Channel for the actual notifications from servers the user is a member of
        createNotificationChannel(
            SERVER_CHANNEL_ID,
            getString(R.string.channel_name),
            getString(R.string.channel_description),
            NotificationManager.IMPORTANCE_HIGH
        )

        // Channel for showing the foreground notification for the listener that listens for notifications
        // (Thanks ~~Obama~~ Google!)
        createNotificationChannel(
            SERVICE_CHANNEL_ID,
            "Block Background Processes",
            "This channel shows you what background processes (such as services listening for notifications from the Revolt server) Neo is running."
        )
    }

    private fun createNotificationChannel(
        channelId: String,
        name: String,
        descriptionText: String,
        importance: Int = NotificationManager.IMPORTANCE_LOW
    ) {
        val mChannel = NotificationChannel(channelId, name, importance)
        mChannel.description = descriptionText
        mChannel.enableLights(true)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
    }
}

@Module
@InstallIn(SingletonComponent::class)
class MyApplicationModule {

    @Provides
    fun providesMainApplicationInstance(@ApplicationContext context: Context): MainApplication {
        return context as MainApplication
    }
}