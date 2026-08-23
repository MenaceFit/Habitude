package com.menacefit.habitude

import android.app.Application
import androidx.work.Configuration
import com.menacefit.habitude.di.AppContainer
import com.menacefit.habitude.notifications.HabitudeWorkerFactory
import com.menacefit.habitude.notifications.NotificationChannels
import com.menacefit.habitude.util.CrashHandler

class HabitudeApplication : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        container = AppContainer(this)
        NotificationChannels.ensureCreated(this)
    }

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(HabitudeWorkerFactory(container))
            .build()
}
