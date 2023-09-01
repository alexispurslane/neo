package io.github.alexispurslane.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Intent(context, WebSocketNotificationService::class.java).apply {
                action = Actions.START.name
                context?.startForegroundService(this)
            }
        }
    }
}