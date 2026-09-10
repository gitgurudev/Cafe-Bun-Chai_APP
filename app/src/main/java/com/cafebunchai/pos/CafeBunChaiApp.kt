package com.cafebunchai.pos

import android.app.Application
import com.cafebunchai.pos.data.AppContainer
import com.cafebunchai.pos.notify.StockAlerts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class CafeBunChaiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        StockAlerts.createChannel(this)
        container = AppContainer(this)
        runBlocking(Dispatchers.IO) {
            container.db.seedIfEmpty()
        }
    }
}
