package com.portalhost.app.server

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.util.zip.ZipFile

object JarAnalyzer {
    private const val TAG = "JarAnalyzer"

    /** Copy a content URI to a temp file and try to detect MC version from the JAR. */
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
                // Try version.json (Mojang vanilla format)
                val versionEntry = zip.getEntry("version.json")
                if (versionEntry != null) {
                    val json = zip.getInputStream(versionEntry).bufferedReader().use { it.readText() }
                    // Parse: {"id": "1.21", ...}
                    val idMatch = Regex(""""id"\s*:\s*"([^"]+)""").find(json)
                    if (idMatch != null) return idMatch.groupValues[1]
                }

                // Try META-INF/MANIFEST.MF
                val manifestEntry = zip.getEntry("META-INF/MANIFEST.MF")
                if (manifestEntry != null) {
                    val manifest = zip.getInputStream(manifestEntry).bufferedReader().use { it.readText() }
                    val implVersion = Regex("""Implementation-Version:\s*(.+)""").find(manifest)
                    if (implVersion != null) return implVersion.groupValues[1].trim()
                    val specVersion = Regex("""Specification-Version:\s*(.+)""").find(manifest)
                    if (specVersion != null) return specVersion.groupValues[1].trim()
                }

                // Try META-INF/versions.list (Paper format: MCVer|PatchVer|...)
                val versionsList = zip.getEntry("META-INF/versions.list")
                if (versionsList != null) {
                    val firstLine = zip.getInputStream(versionsList).bufferedReader().use { it.readLine() }
                    if (firstLine != null) {
                        val parts = firstLine.split("|")
                        if (parts.size >= 1 && parts[0].isNotBlank()) return parts[0]
                    }
                }

                // Try fabric.mod.json
                val fabricEntry = zip.getEntry("fabric.mod.json")
                if (fabricEntry != null) {
                    val json = zip.getInputStream(fabricEntry).bufferedReader().use { it.readText() }
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
