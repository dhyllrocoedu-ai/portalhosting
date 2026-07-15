pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.multiplatform") {
                useVersion("2.0.20")
            }
            if (requested.id.id == "com.android.application") {
                useVersion("8.5.0")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.compose") {
                useVersion("2.0.20")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.serialization") {
                useVersion("2.0.20")
            }
            if (requested.id.id == "org.jetbrains.compose") {
                useVersion("1.6.11")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}

rootProject.name = "PortalHost"

include("composeApp")