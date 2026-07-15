package com.portalhost.di

import com.portalhost.db.DatabaseDriverFactory
import com.portalhost.db.DatabaseRepository
import com.portalhost.filesystem.FileSystem
import com.portalhost.log.LogRepository
import com.portalhost.preferences.Preferences
import com.portalhost.server.ActivityLog
import com.portalhost.server.ServerDownloader
import com.portalhost.server.ServerManager
import com.portalhost.server.providers.FabricProvider
import com.portalhost.server.providers.FoliaProvider
import com.portalhost.server.providers.ForgeProvider
import com.portalhost.server.providers.NeoForgeProvider
import com.portalhost.server.providers.PaperProvider
import com.portalhost.server.providers.PurpurProvider
import com.portalhost.server.providers.ServerProviderRegistry
import com.portalhost.server.providers.VanillaProvider
import com.portalhost.uinotify.ToastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

fun commonModule() = module {
    single { ServerProviderRegistry() }

    single { ActivityLog() }

    single { ToastManager() }

    single { LogRepository() }

    single { PaperProvider() }
    single { FoliaProvider() }
    single { PurpurProvider() }
    single { VanillaProvider() }
    single { FabricProvider() }
    single { ForgeProvider() }
    single { NeoForgeProvider() }

    single { FileSystem(preferences = getOrNull()) }

    single {
        val customDir = getOrNull<Preferences>()?.dataDirectory?.value?.takeIf { it.isNotBlank() }
        DatabaseDriverFactory(customDataDir = customDir).createDatabase()
    }

    single { ServerDownloader(registry = get(), serversDir = get<FileSystem>().getServersDirBlocking()) }

    single {
        ServerManager(
            downloader = get(),
            processManager = get(),
            serversDir = get<FileSystem>().getServersDirBlocking(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            database = get(),
            jdkManager = get(),
        )
    }
}

fun initKoin(platformModule: Module) {
    startKoin {
        modules(commonModule(), platformModule)
    }
}
