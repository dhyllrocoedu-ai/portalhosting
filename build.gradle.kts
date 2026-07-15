plugins {
    id("org.jetbrains.kotlin.multiplatform") apply false
    id("com.android.application") apply false
    id("org.jetbrains.kotlin.plugin.compose") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("org.jetbrains.compose") apply false
}

allprojects {
    group = "com.portalhost"
    version = "1.0.0"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}