plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.11.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
}

group = "com.portalhost"
version = "5.0.16"

kotlin {
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
                implementation("io.ktor:ktor-client-core:3.0.0")
                implementation("io.ktor:ktor-client-okhttp:3.0.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.0")
                implementation("io.ktor:ktor-client-logging:3.0.0")
                implementation("com.russhwolf:multiplatform-settings:1.0.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                implementation("io.insert-koin:koin-core:4.0.0")
                implementation("io.insert-koin:koin-compose:4.0.0")
                implementation("io.github.microutils:kotlin-logging:3.0.5")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.materialIconsExtended)
                implementation(compose.uiUtil)
                implementation("org.jetbrains.compose.ui:ui-util:1.11.0")
                implementation("org.xerial:sqlite-jdbc:3.45.3.0")
                implementation("io.insert-koin:koin-core:4.0.0")
                implementation("androidx.navigation:navigation-compose:2.9.0")
                implementation("ch.qos.logback:logback-classic:1.5.6")
                implementation("androidx.compose.ui:ui-text:1.7.2")
            }
        }
        configurations.all {
            resolutionStrategy {
                force("androidx.compose.ui:ui-text:1.7.2")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.portalhost.desktop.DesktopMainKt"
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
            )
            packageName = "PortalHost"
            packageVersion = "5.0.16"
            description = "Minecraft Java Edition Server Manager"
            vendor = "PortalHost"
            modules("java.sql", "java.naming", "java.management", "java.net.http")
            windows {
                menuGroup = "PortalHost"
                menu = true
                shortcut = true
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(listOf("-opt-in=kotlin.RequiresOptIn"))
    }
}