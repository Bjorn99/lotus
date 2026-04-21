package com.dn0ne.player

import android.app.Application
import com.dn0ne.player.app.data.db.RealmToRoomMigrator
import com.dn0ne.player.app.di.playerModule
import com.dn0ne.player.core.crash.CrashReporter
import com.dn0ne.player.core.di.appModule
import com.dn0ne.player.setup.di.setupModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PlayerApp: Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val realmToRoomMigrator: RealmToRoomMigrator by inject()

    override fun onCreate() {
        super.onCreate()

        // Install the on-device crash reporter before anything else so early
        // init failures (including Koin startup) are captured to disk.
        CrashReporter(this).install()

        startKoin {
            androidContext(this@PlayerApp)
            modules(appModule, setupModule, playerModule)
        }

        // One-shot copy of the legacy Realm store into Room. Off the main
        // thread, no-op after the first successful run. See RealmToRoomMigrator.
        applicationScope.launch {
            realmToRoomMigrator.migrateIfNeeded()
        }
    }
}
