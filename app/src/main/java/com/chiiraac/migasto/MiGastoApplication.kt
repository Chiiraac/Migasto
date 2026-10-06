package com.chiiraac.migasto

import android.app.Application
import com.chiiraac.migasto.notifications.Notifications
import com.chiiraac.migasto.util.TempFiles
import kotlinx.coroutines.launch

class MiGastoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.applicationScope.launch { TempFiles.pruneStale(this@MiGastoApplication) }
        if (container.isCloud) Notifications.createChannels(this)
    }
}
