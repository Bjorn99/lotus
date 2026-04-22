package com.dn0ne.player

import android.app.Application
import com.dn0ne.player.app.di.playerModule
import com.dn0ne.player.core.crash.CrashReporter
import com.dn0ne.player.core.di.appModule
import com.dn0ne.player.setup.di.setupModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PlayerApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Install the on-device crash reporter before anything else so early
        // init failures (including Koin startup) are captured to disk.
        CrashReporter(this).install()

        startKoin {
            androidContext(this@PlayerApp)
            modules(appModule, setupModule, playerModule)
        }
    }
}
