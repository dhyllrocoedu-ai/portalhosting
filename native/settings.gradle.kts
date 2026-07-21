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
            if (requested.id.id == "com.squareup.sqldelight") {
                useVersion("1.5.3")
            }
            if (requested.id.id == "org.jetbrains.kotlin.multiplatform") {
                useVersion("2.1.20")
            }
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useVersion("2.1.20")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.compose") {
                useVersion("2.1.20")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.serialization") {
                useVersion("2.0.20")
            }
            if (requested.id.id == "com.android.application") {
                useVersion("8.8.2")
            }
            if (requested.id.id == "org.jetbrains.compose.desktop") {
                useVersion("1.5.14")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    }
}

rootProject.name = "PortalHost"
include(":app")
// include(":composeApp")
// project(":composeApp").projectDir = file("../composeApp")