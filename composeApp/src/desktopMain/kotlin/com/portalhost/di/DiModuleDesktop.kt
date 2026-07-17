package com.portalhost.di

import com.portalhost.filesystem.FileSystem
import com.portalhost.java.JdkManager
import com.portalhost.preferences.Preferences
import com.portalhost.process.ProcessManager
import com.portalhost.server.TunnelManager
import org.koin.dsl.module

fun desktopModule() = module {
    single<FileSystem> { FileSystem(preferences = get()) }
    single<ProcessManager> { ProcessManager() }
    single<Preferences> { Preferences() }
    single<JdkManager> { JdkManager(fileSystem = get()) }
    single<TunnelManager> { TunnelManager(fileSystem = get()) }
}
