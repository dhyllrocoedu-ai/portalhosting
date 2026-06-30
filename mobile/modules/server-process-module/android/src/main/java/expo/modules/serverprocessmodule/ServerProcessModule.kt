package expo.modules.serverprocessmodule

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.io.*
import java.util.concurrent.TimeUnit

class ServerProcessModule : Module() {
  private var process: Process? = null

  override fun definition() = ModuleDefinition {
    Name("ServerProcessModule")

    AsyncFunction("startProcess") { jarPath: String, args: Array<String> ->
      val proc = ProcessBuilder("java", *args)
        .directory(File(jarPath).parentFile)
        .redirectErrorStream(false)
        .start()
      process = proc

      Thread {
        try {
          proc.inputStream.bufferedReader().use { reader ->
            reader.lines().forEach { line ->
              sendEvent("onStdout", mapOf("data" to line))
            }
          }
        } catch (_: IOException) {}
      }.apply { isDaemon = true }.start()

      Thread {
        try {
          proc.errorStream.bufferedReader().use { reader ->
            reader.lines().forEach { line ->
              sendEvent("onStderr", mapOf("data" to line))
            }
          }
        } catch (_: IOException) {}
      }.apply { isDaemon = true }.start()

      Thread {
        try {
          val exitCode = proc.waitFor()
          sendEvent("onExit", mapOf("exitCode" to exitCode))
          process = null
        } catch (_: InterruptedException) {}
      }.apply { isDaemon = true }.start()
    }

    AsyncFunction("stopProcess") {
      process?.let { proc ->
        try {
          proc.outputStream.write("stop\n".toByteArray())
          proc.outputStream.flush()
          if (!proc.waitFor(10, TimeUnit.SECONDS)) {
            proc.destroyForcibly()
          }
        } catch (_: IOException) {
          proc.destroyForcibly()
        }
        process = null
      }
    }

    AsyncFunction("writeStdin") { command: String ->
      process?.let { proc ->
        proc.outputStream.write(command.toByteArray())
        proc.outputStream.flush()
      }
    }

    AsyncFunction("isRunning") {
      process?.isAlive ?: false
    }

    Events("onStdout", "onStderr", "onExit")
  }
}
