package expo.modules.serverprocessmodule

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import java.io.*
import java.net.ServerSocket
import java.net.SocketTimeoutException

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
          cleanupTcp()
          throw e
        }
      } else {
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
  } catch (_: PackageManager.NameNotFoundException) { false }

  // ── Termux TCP mode ──────────────────────────────────────

  private fun startViaTermux(ctx: Context, jarPath: String, javaPath: String, jvmArgs: Array<String>) {
    val jarFile = File(jarPath)
    val serverDir = jarFile.parentFile?.absolutePath ?: "."
    val jarName = jarFile.name
    val serverName = File(serverDir).name

    // 1. Copy files to shared storage so Termux can read them
    val sharedDir = ensureSharedFiles(ctx, jarPath, serverDir, serverName)

    // 2. Create TCP server on loopback
    val ss = ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
    serverSocket = ss
    val port = ss.localPort

    // 3. Accept connection in background thread
    Thread {
      try {
        ss.soTimeout = 30000
        val client = ss.accept()
        clientSocket = client
        val rdr = BufferedReader(InputStreamReader(client.inputStream, "UTF-8"))
        var line: String? = null
        while (!cleanupRequested && rdr.readLine().also { line = it } != null) {
          sendEvent("onStdout", mapOf("data" to line!!))
        }
      } catch (_: SocketTimeoutException) {
        sendEvent("onStdout", mapOf("data" to "[PortalHost] Timed out waiting for Termux connection"))
      } catch (_: IOException) {
        // connection closed normally
      }

      if (!cleanupRequested) {
        sendEvent("onExit", mapOf("exitCode" to 0))
      }
      cleanupTcp()
    }.apply { isDaemon = true }.start()

    // 4. Build command
    val ramArg = jvmArgs.find { it.startsWith("-Xmx") } ?: "-Xmx1024M"
    val termuxJava = resolveTermuxJava(javaPath)
    val shellCmd = buildString {
      append("cd '${sharedDir}' && ")
      append("exec '${termuxJava}' ${ramArg} -jar '${jarName}' nogui ")
      append("0<>/dev/tcp/127.0.0.1/${port} 1>&0 2>&0")
    }

    // 5. Send RUN_COMMAND intent
    val intent = Intent("com.termux.RUN_COMMAND_SERVICE").apply {
      `package` = "com.termux"
      putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
      putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", shellCmd))
      putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
      putExtra("com.termux.RUN_COMMAND_WORKDIR", sharedDir)
    }
    ctx.startService(intent)
  }

  /** Use user-configured path; if bare "java", expand via Termux's resolved path. */
  private fun resolveTermuxJava(userPath: String): String {
    if (userPath.startsWith("/")) return userPath
    return "/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk/bin/java"
  }

  // ── MediaStore file sharing ──────────────────────────────

  private fun ensureSharedFiles(ctx: Context, jarPath: String, serverDir: String, serverName: String): String {
    val relativePath = "Download/PortalHost/${serverName}/"
    val sharedDir = "/storage/emulated/0/Download/PortalHost/${serverName}"
    val cr = ctx.contentResolver
    val jarFile = File(jarPath)

    fun putFile(displayName: String, mime: String, src: File) {
      if (!src.exists()) return
      if (mediaStoreExists(cr, displayName, relativePath, src.length())) return
      deleteMediaFile(cr, displayName, relativePath)
      val v = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, displayName)
        put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
        put(MediaStore.Downloads.MIME_TYPE, mime)
        put("is_pending", 1)
      }
      val uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
        ?: throw Exception("MediaStore insert failed for $displayName")
      cr.openOutputStream(uri)?.use { out -> src.inputStream().use { input -> input.copyTo(out) } }
        ?: throw Exception("MediaStore write failed for $displayName")
      v.clear(); v.put("is_pending", 0)
      cr.update(uri, v, null, null)
    }

    putFile("server.jar", "application/java-archive", jarFile)
    putFile("eula.txt", "text/plain", File(serverDir, "eula.txt"))
    putFile("server.properties", "text/plain", File(serverDir, "server.properties"))

    return sharedDir
  }

  private fun mediaStoreExists(cr: ContentResolver, name: String, path: String, size: Long): Boolean {
    val cols = arrayOf(MediaStore.Downloads.SIZE)
    val sel = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
    cr.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cols, sel, arrayOf(name, path), null)?.use { c ->
      if (c.moveToFirst()) return c.getLong(c.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)) == size
    }
    return false
  }

  private fun deleteMediaFile(cr: ContentResolver, name: String, path: String) {
    val sel = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
    cr.delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI, sel, arrayOf(name, path))
  }

  // ── Kill helper ──────────────────────────────────────────

  private fun killJavaInTermux(ctx: Context) {
    try {
      val i = Intent("com.termux.RUN_COMMAND_SERVICE").apply {
        `package` = "com.termux"
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
