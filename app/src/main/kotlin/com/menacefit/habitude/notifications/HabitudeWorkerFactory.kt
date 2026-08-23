package com.menacefit.habitude.notifications

import android.content.Context
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.menacefit.habitude.di.AppContainer

/** Lets [HabitReminderWorker] receive the app's [AppContainer] without a reflection-based DI framework. */
class HabitudeWorkerFactory(private val container: AppContainer) : WorkerFactory() {
    override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters) =
        when (workerClassName) {
            HabitReminderWorker::class.java.name -> HabitReminderWorker(appContext, workerParameters, container)
            else -> null
        }
}
