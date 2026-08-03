tasks.withType<org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask>().configureEach {
    println("Task: $name")
    println("Properties: ${properties.keys.joinToString(", ")}")
}