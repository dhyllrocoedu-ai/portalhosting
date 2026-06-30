package com.portalhost.app.server

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.io.*

class ServerProcessModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  private var process: Process? = null

  override fun getName(): String = "ServerProcessModule"

  @ReactMethod
  fun startProcess(jarPath: String, args: ReadableArray, promise: Promise) {
    try {
      val cmd = args.toArrayList().map { it.toString() }.toTypedArray()
      val pb = ProcessBuilder(*cmd)
        .directory(File(jarPath).parentFile)
        .redirectErrorStream(true)

      val proc = pb.start()
      process = proc
      promise.resolve(null)

      Thread {
        try {
          proc.inputStream.bufferedReader().use { reader ->
            reader.lines().forEach { line ->
              sendEvent("onStdout", line)
            }
          }
        } catch (_: IOException) {}
      }.apply { isDaemon = true }.start()

      Thread {
        try {
          proc.errorStream.bufferedReader().use { reader ->
            reader.lines().forEach { line ->
              sendEvent("onStderr", line)
            }
          }
        } catch (_: IOException) {}
      }.apply { isDaemon = true }.start()

      Thread {
        try {
          val exitCode = proc.waitFor()
          sendEvent("onExit", exitCode)
          process = null
        } catch (_: InterruptedException) {}
      }.apply { isDaemon = true }.start()
    } catch (e: Exception) {
      promise.reject("PROCESS_START_ERROR", e.message, e)
    }
  }

  @ReactMethod
  fun stopProcess(promise: Promise) {
    try {
      process?.let { proc ->
        try {
          proc.outputStream.write("stop\n".toByteArray())
          proc.outputStream.flush()
          if (!proc.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
            proc.destroyForcibly()
          }
        } catch (_: IOException) {
          proc.destroyForcibly()
        }
        process = null
      }
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("PROCESS_STOP_ERROR", e.message, e)
    }
  }

  @ReactMethod
  fun writeStdin(command: String, promise: Promise) {
    try {
      process?.let { proc ->
        proc.outputStream.write(command.toByteArray())
        proc.outputStream.flush()
      }
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("STDIN_ERROR", e.message, e)
    }
  }

  @ReactMethod
  fun isRunning(promise: Promise) {
    promise.resolve(process?.isAlive ?: false)
  }

  private fun sendEvent(eventName: String, data: Any) {
    reactApplicationContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, data)
  }
}
