package expo.modules.serverprocessmodule

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.util.Log
import java.io.*
import java.net.ServerSocket
import java.net.SocketTimeoutException

private const val TAG = "ServerProcessModule"

class ServerProcessModule : Module() {
  private var serverSocket: ServerSocket? = null
  private var clientSocket: java.net.Socket? = null
  private var process: Process? = null
  private var cleanupRequested = false

  override fun definition() = ModuleDefinition {
    Name("ServerProcessModule")

    AsyncFunction("startProcess") { jarPath: String, args: Array<String> ->
      val context = appContext.reactContext ?: throw Exception("No ReactContext")
      val javaPath = args[0]
      val jvmArgs = args.sliceArray(1 until args.size)
      cleanupRequested = false

      if (isTermuxInstalled(context)) {
        try {
          startViaTermux(context, jarPath, javaPath, jvmArgs)
        } catch (e: Exception) {
          Log.e(TAG, "startViaTermux failed: ${e.message}", e)
          sendEvent("onStdout", mapOf("data" to "[PortalHost] startViaTermux failed: ${e.message}"))
          cleanupTcp()
          throw e
        }
      } else {
        Log.i(TAG, "Termux not found, using direct ProcessBuilder")
        startViaDirect(jarPath, javaPath, jvmArgs)
      }
    }

    AsyncFunction("stopProcess") {
      val context = appContext.reactContext
      cleanupRequested = true

      if (clientSocket != null && !clientSocket!!.isClosed) {
        try {
          clientSocket!!.getOutputStream().write("stop\n".toByteArray())
          clientSocket!!.getOutputStream().flush()
          Thread.sleep(8000)
        } catch (_: Exception) {}
      }

      if (context != null && isTermuxInstalled(context)) {
        killJavaInTermux(context)
      }

      process?.destroyForcibly()
      cleanupTcp()
    }

    AsyncFunction("writeStdin") { command: String ->
      if (clientSocket != null && !clientSocket!!.isClosed) {
        try {
          clientSocket!!.getOutputStream().write(command.toByteArray())
          clientSocket!!.getOutputStream().flush()
        } catch (_: IOException) {}
      } else if (process != null) {
        try {
          process!!.outputStream.write(command.toByteArray())
          process!!.outputStream.flush()
        } catch (_: IOException) {}
      }
    }

    AsyncFunction("isRunning") {
      if (clientSocket != null) {
        return@AsyncFunction try {
          !clientSocket!!.isClosed && clientSocket!!.isConnected
        } catch (_: Exception) { false }
      }
      process?.isAlive ?: false
    }

    Events("onStdout", "onStderr", "onExit")
  }

  // ── helpers ──────────────────────────────────────────────

  private fun isTermuxInstalled(ctx: Context): Boolean = try {
    ctx.packageManager.getPackageInfo("com.termux", PackageManager.PackageInfoFlags.of(0))
    true
  } catch (_: PackageManager.NameNotFoundException) {
    Log.w(TAG, "Termux not installed, falling back to direct ProcessBuilder")
    false
  }

  private fun sendError(msg: String) {
    Log.e(TAG, msg)
    sendEvent("onStdout", mapOf("data" to "[PortalHost] $msg"))
  }

  // ── Termux TCP mode ──────────────────────────────────────

  private fun startViaTermux(ctx: Context, jarPath: String, javaPath: String, jvmArgs: Array<String>) {
    val jarFile = File(jarPath)
    val serverDir = jarFile.parentFile?.absolutePath ?: "."
    val jarName = jarFile.name
    val serverName = File(serverDir).name

    // 1. Copy files to shared storage so Termux can read them
    val sharedDir = ensureSharedFiles(ctx, jarPath, serverDir, serverName)
    val prefixedJar = "PortalHost_${serverName}_server.jar"
    val ss = ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
    serverSocket = ss
    val port = ss.localPort

    // 3. Accept connection in background thread
    Thread {
      try {
        ss.soTimeout = 30000
        Log.i(TAG, "TCP server listening on 127.0.0.1:$port, waiting for Termux...")
        val client = ss.accept()
        Log.i(TAG, "Termux TCP connection established from ${client.inetAddress}:${client.port}")
        clientSocket = client
        val rdr = BufferedReader(InputStreamReader(client.inputStream, "UTF-8"))
        var line: String? = null
        while (!cleanupRequested && rdr.readLine().also { line = it } != null) {
          sendEvent("onStdout", mapOf("data" to line!!))
        }
      } catch (e: SocketTimeoutException) {
        Log.w(TAG, "TCP accept timed out (30s) - Termux never connected")
        sendEvent("onStdout", mapOf("data" to "[PortalHost] Timed out waiting for Termux connection"))
      } catch (e: IOException) {
        Log.i(TAG, "TCP connection closed (${e.message})")
      } catch (e: Exception) {
        Log.e(TAG, "TCP thread error: ${e.message}", e)
      }

      if (!cleanupRequested) {
        sendEvent("onExit", mapOf("exitCode" to 0))
      }
      cleanupTcp()
    }.apply { isDaemon = true }.start()

    // 4. Build shell command – single-FIFO pipeline IPC.
    //    nc reads FIFO → TCP; nc stdout → pipe → java stdin;
    //    java stdout/stderr → FIFO → nc stdin → TCP back to PortalHost.
    val termuxJava = resolveTermuxJava(javaPath)
    val jvmFlags = jvmArgs.filter { it.startsWith("-") && it != "-jar" }
    val targetDir = "/storage/emulated/0/data/PortalHost/${serverName}"
    val shellCmd = buildString {
      append("F=/data/data/com.termux/files/usr/tmp/ph_fifo.\$\$; ")
      append("mkfifo \"\$F\"; ")
      append("mkdir -p '${targetDir}'; ")
      append("cd '${targetDir}'; ")
      append("/system/bin/netcat -q0 127.0.0.1 ${port} < \"\$F\" | ")
      append("'${termuxJava}' ${jvmFlags.joinToString(" ")} -jar 'server.jar' nogui > \"\$F\" 2>&1; ")
      append("rm -f \"\$F\"")
    }

    // 5. Send RUN_COMMAND intent
    Log.i(TAG, "Starting Termux with: ${shellCmd.take(350)}…")
    val intent = Intent("com.termux.RUN_COMMAND").apply {
      component = ComponentName("com.termux", "com.termux.app.RunCommandService")
      putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
      putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", shellCmd))
    }
    try {
      val result = ctx.startService(intent)
      if (result != null) {
        Log.i(TAG, "Termux RUN_COMMAND_SERVICE intent sent (result=$result)")
      } else {
        Log.e(TAG, "Termux startService returned null — service not found")
        sendError("Termux RunCommandService not found")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to send Termux intent: ${e.message}", e)
      sendError("Failed to start Termux: ${e.message}")
      throw e
    }
  }

  /** Use user-configured path; if bare "java", expand via Termux's resolved path. */
  private fun resolveTermuxJava(userPath: String): String {
    if (userPath.startsWith("/")) return userPath
    return "/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk/bin/java"
  }

  // ── MediaStore file sharing ──────────────────────────────

  /** API 36 only allows "Download" as primary directory in RELATIVE_PATH.
   *  Use flat filenames prefixed with server name in the Downloads root. */
  private fun ensureSharedFiles(ctx: Context, jarPath: String, serverDir: String, serverName: String): String {
    val sharedDir = "/storage/emulated/0/Download"
    val cr = ctx.contentResolver

    fun pfx(name: String) = "PortalHost_${serverName}_${name}"

    fun put(src: File, localName: String, mime: String) {
      if (!src.exists()) { Log.w(TAG, "ensureSharedFiles: src missing: ${src.absolutePath} — skipping"); return }
      val displayName = pfx(localName)
      if (mediaFileExists(cr, displayName, src.length())) { Log.i(TAG, "ensureSharedFiles: $displayName exists, skipping"); return }
      deleteMediaFile(cr, displayName)
      val v = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, displayName)
        put(MediaStore.Downloads.RELATIVE_PATH, "")
        put(MediaStore.Downloads.MIME_TYPE, mime)
        put("is_pending", 1)
      }
      val uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
        ?: throw Exception("MediaStore insert failed for $displayName")
      cr.openOutputStream(uri)?.use { out -> src.inputStream().use { input -> input.copyTo(out) } }
        ?: throw Exception("MediaStore write failed for $displayName")
      v.clear(); v.put("is_pending", 0)
      cr.update(uri, v, null, null)
      Log.i(TAG, "ensureSharedFiles: wrote $displayName to ${sharedDir}/${displayName}")
    }

    // If eula.txt or server.properties are missing from the app directory, create defaults.
    // This handles the case where the server was created pre-v1.1.4 or app data was wiped.
    val eulaFile = File(serverDir, "eula.txt")
    if (!eulaFile.exists()) {
      eulaFile.writeText("eula=true\n")
      Log.i(TAG, "Created default eula.txt at ${eulaFile.absolutePath}")
    }
    val propsFile = File(serverDir, "server.properties")
    if (!propsFile.exists()) {
      propsFile.writeText(
        "max-players=20\nonline-mode=false\nlevel-seed=\ngamemode=survival\ndifficulty=easy\n" +
        "spawn-protection=16\npvp=true\nallow-nether=true\nenable-command-block=false\nmotd=A PortalHost Server\n"
      )
      Log.i(TAG, "Created default server.properties at ${propsFile.absolutePath}")
    }

    put(File(jarPath), "server.jar", "application/java-archive")
    put(File(serverDir, "eula.txt"), "eula.txt", "text/plain")
    put(File(serverDir, "server.properties"), "server.properties", "text/plain")

    return sharedDir
  }

  private fun mediaFileExists(cr: ContentResolver, name: String, size: Long): Boolean {
    val cols = arrayOf(MediaStore.Downloads.SIZE)
    val sel = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
    cr.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cols, sel, arrayOf(name), null)?.use { c ->
      if (c.moveToFirst()) return c.getLong(c.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)) == size
    }
    return false
  }

  private fun deleteMediaFile(cr: ContentResolver, name: String) {
    val sel = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
    cr.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, sel, arrayOf(name))
  }

  // ── Kill helper ──────────────────────────────────────────

  private fun killJavaInTermux(ctx: Context) {
    try {
      val i = Intent("com.termux.RUN_COMMAND").apply {
        component = ComponentName("com.termux", "com.termux.app.RunCommandService")
        putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
        putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", "killall java 2>/dev/null || true"))
        putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
      }
      ctx.startService(i)
    } catch (_: Exception) {}
  }

  // ── Direct ProcessBuilder fallback ───────────────────────

  private fun startViaDirect(jarPath: String, javaPath: String, jvmArgs: Array<String>) {
    val proc = ProcessBuilder(javaPath, *jvmArgs)
      .directory(File(jarPath).parentFile)
      .redirectErrorStream(true)
      .start()
    process = proc

    Thread {
      try { proc.inputStream.bufferedReader().lines().forEach { sendEvent("onStdout", mapOf("data" to it)) } }
      catch (_: IOException) {}
    }.apply { isDaemon = true }.start()

    Thread {
      try { sendEvent("onExit", mapOf("exitCode" to proc.waitFor())); process = null }
      catch (_: InterruptedException) {}
    }.apply { isDaemon = true }.start()
  }

  // ── Cleanup ──────────────────────────────────────────────

  private fun cleanupTcp() {
    try { clientSocket?.close() } catch (_: Exception) {}
    try { serverSocket?.close() } catch (_: Exception) {}
    clientSocket = null; serverSocket = null
  }
}
