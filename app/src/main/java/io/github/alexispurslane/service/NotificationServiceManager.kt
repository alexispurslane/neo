package io.github.alexispurslane.service

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationServiceManager(private val context: Context) {
    companion object {
        const val WORK_NAME_ONCE = "NotificationServiceStartWorkerOnce"

        fun start(context: Context) {
            val manager = NotificationServiceManager(context)
            manager.start()
        }
    }

    fun start() {
        Log.d(
            "NOTIF SERVICE MANAGER",
            "Enqueuing work to refresh notification service"
        )
        val workManager = WorkManager.getInstance(context)
        val startServiceRequest =
            OneTimeWorkRequest.Builder(ServiceStartWorker::class.java).build()
        workManager.enqueueUniqueWork(
            WORK_NAME_ONCE,
            ExistingWorkPolicy.KEEP,
            startServiceRequest
        )
    }

    fun restart() {
        Intent(context, NotificationService::class.java).also {
            context.stopService(it)
        }
    }

    class ServiceStartWorker(
        private val context: Context,
        params: WorkerParameters
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val id = this.id
            if (context.applicationContext !is Application) {
                Log.d(
                    "NOTIF SERVICE MANAGER",
                    "ServiceStartWorker: Failed, no application found"
                )
                return Result.failure()
            }

            withContext(Dispatchers.IO) {
                val app = context.applicationContext as Application
                Intent(context, NotificationService::class.java).apply {
                    action = Actions.START.name
                    ContextCompat.startForegroundService(context, this)
                }
            }

            return Result.success()
        }
    }
}