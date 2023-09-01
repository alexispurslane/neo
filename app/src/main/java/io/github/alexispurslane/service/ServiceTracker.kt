package io.github.alexispurslane.service

import android.content.Context
import android.content.Context.MODE_PRIVATE

enum class ServiceState {
    STARTED,
    STOPPED
}

const val SERVICE_PREF_NAME = "NOTIFICATION_SERVICE_WATCHER_KEY"
const val SERVICE_PREF_KEY = "NOTIFICATION_SERVICE_STATE"

fun setServiceState(context: Context, state: ServiceState) {
    val sharedPrefs =
        context.getSharedPreferences(SERVICE_PREF_NAME, MODE_PRIVATE)
    sharedPrefs.edit().apply {
        putString(SERVICE_PREF_KEY, state.name)
        apply()
    }
}

fun getServiceState(context: Context): ServiceState {
    val sharedPrefs =
        context.getSharedPreferences(SERVICE_PREF_NAME, MODE_PRIVATE)
    val value =
        sharedPrefs.getString(SERVICE_PREF_KEY, ServiceState.STOPPED.name)
            ?: ServiceState.STOPPED.name
    return ServiceState.valueOf(value)
}
