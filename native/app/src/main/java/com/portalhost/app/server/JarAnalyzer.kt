package com.portalhost.app.server

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.zip.ZipFile

object JarAnalyzer {
    private const val TAG = "JarAnalyzer"

    fun detectVersion(context: Context, jarUri: Uri): String {
        return try {
            val tempDir = File(context.cacheDir, "jar_analysis")
            tempDir.mkdirs()
            val tempFile = File(tempDir, "temp.jar")
            context.contentResolver.openInputStream(jarUri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (!tempFile.exists()) return ""
            val version = readVersionFromJar(tempFile)
            tempFile.delete()
            version
        } catch (e: Exception) {
            Log.w(TAG, "Version detection failed: ${e.message}")
            ""
        }
    }

    fun readVersionFromJar(jarFile: File): String {
        return try {
            ZipFile(jarFile).use { zip ->
                // Try META-INF/MANIFEST.MF first (works for Paper + many others)
                val manifestEntry = zip.getEntry("META-INF/MANIFEST.MF")
                if (manifestEntry != null) {
                    val manifest = zip.getInputStream(manifestEntry).bufferedReader().use { it.readText() }
                    val implVersion = Regex("""Implementation-Version:\s*(.+)""").find(manifest)
                    if (implVersion != null) {
                        val ver = implVersion.groupValues[1].trim()
                        // Paper format: "1.21.6-48-main@4d854e6" or "1.21-48-..."
                        val mc = Regex("""(\d+\.\d+(?:\.\d+)?)""").find(ver)
                        if (mc != null) return mc.groupValues[1]
                        return ver
                    }
                    val specVersion = Regex("""Specification-Version:\s*(.+)""").find(manifest)
                    if (specVersion != null) return specVersion.groupValues[1].trim()
                }

                // Try version.json at root (Mojang vanilla format)
                val versionEntry = zip.getEntry("version.json")
                if (versionEntry != null) {
                    val json = zip.getInputStream(versionEntry).bufferedReader().use { it.readText() }
                    val idMatch = Regex(""""id"\s*:\s*"([^"]+)""").find(json)
                    if (idMatch != null) return idMatch.groupValues[1]
                }

                // Paperclip: look inside META-INF/versions.list (old format) or patch.jar (new format)
                val versionsList = zip.getEntry("META-INF/versions.list")
                if (versionsList != null) {
                    val firstLine = zip.getInputStream(versionsList).bufferedReader().use { it.readLine() }
                    if (firstLine != null) {
                        val parts = firstLine.split("|")
                        if (parts.size >= 1 && parts[0].isNotBlank()) return parts[0]
                    }
                }

                // Paperclip new format: version.json is inside META-INF/patch.jar
                val patchEntry = zip.getEntry("META-INF/patch.jar")
                if (patchEntry != null) {
                    val patchFile = File.createTempFile("patch", ".jar")
                    try {
                        zip.getInputStream(patchEntry).use { input ->
                            patchFile.outputStream().use { output -> input.copyTo(output) }
                        }
                        val innerVersion = readVersionFromJar(patchFile)
                        if (innerVersion.isNotBlank()) return innerVersion
                    } finally {
                        patchFile.delete()
                    }
                }

                // Try fabric.mod.json
                val fabricEntry = zip.getEntry("fabric.mod.json")
                if (fabricEntry != null) {
                    val json = zip.getInputStream(fabricEntry).bufferedReader().use { it.readText() }
                    // Fabric has a "minecraft" dependency with a "version" in "depends"
                    val dependsMatch = Regex(""""depends"\s*:\s*\{[^}]*"minecraft"\s*:\s*"([^"]+)""").find(json)
                    if (dependsMatch != null) return dependsMatch.groupValues[1]
                    // Fallback: read the "version" field
                    val versionMatch = Regex(""""version"\s*:\s*"([^"]+)""").find(json)
                    if (versionMatch != null) return versionMatch.groupValues[1]
                }
            }
            ""
        } catch (e: Exception) {
            Log.w(TAG, "JAR analysis failed: ${e.message}")
            ""
        }
    }
}
