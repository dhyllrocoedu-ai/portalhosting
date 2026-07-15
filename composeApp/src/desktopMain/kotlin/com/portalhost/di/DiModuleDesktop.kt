package com.portalhost.di

import com.portalhost.filesystem.FileSystem
import com.portalhost.java.JdkManager
import com.portalhost.preferences.Preferences
import com.portalhost.process.ProcessManager
import org.koin.dsl.module

fun desktopModule() = module {
    single<FileSystem> { FileSystem() }
    single<ProcessManager> { ProcessManager() }
    single<Preferences> { Preferences() }
    single<JdkManager> { JdkManager() }
}
