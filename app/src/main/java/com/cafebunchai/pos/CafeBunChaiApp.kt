package com.cafebunchai.pos

import android.app.Application
import com.cafebunchai.pos.data.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class CafeBunChaiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        runBlocking(Dispatchers.IO) {
            container.db.seedIfEmpty()
        }
    }
}
