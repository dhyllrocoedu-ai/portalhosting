package com.portalhost.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL

data class NetworkInfo(
    val localIp: String = "Unknown",
    val publicIp: String = "",
    val tunnelUrl: String = "",
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val isConnected: Boolean = false
)

class NetworkManager(private val context: Context) {
    private val TAG = "NetworkManager"
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val tunnelFile: File get() = File(context.filesDir, "tunnel_url.txt")

    fun getNetworkInfo(): NetworkInfo {
        return try {
            val network = connectivityManager.activeNetwork ?: return NetworkInfo(tunnelUrl = loadTunnelUrl())
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return NetworkInfo(tunnelUrl = loadTunnelUrl())
            val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val localIp = getLocalIpAddress()
            NetworkInfo(localIp = localIp, isWifi = isWifi, isCellular = isCellular, isConnected = isConnected, tunnelUrl = loadTunnelUrl())
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing permission: ${e.message}")
            NetworkInfo(tunnelUrl = loadTunnelUrl())
        } catch (e: Exception) {
            Log.w(TAG, "Network check failed: ${e.message}")
            NetworkInfo(tunnelUrl = loadTunnelUrl())
        }
    }

    fun fetchPublicIp(): String {
        return try {
            val url = URL("https://api.ipify.org")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            val ip = conn.inputStream.bufferedReader().use { it.readText().trim() }
            conn.disconnect()
            ip
        } catch (e: Exception) {
            Log.w(TAG, "Public IP lookup failed: ${e.message}")
            ""
        }
    }

    fun saveTunnelUrl(url: String) {
        try {
            tunnelFile.writeText(url)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save tunnel URL: ${e.message}")
        }
    }

    fun loadTunnelUrl(): String {
        return try {
            if (tunnelFile.exists()) tunnelFile.readText().trim() else ""
        } catch (_: Exception) { "" }
    }

    private fun getLocalIpAddress(): String {
        val wifiInfo = wifiManager?.connectionInfo
        if (wifiInfo != null) {
            val ipInt = wifiInfo.ipAddress
            if (ipInt != 0) {
                return "${ipInt and 0xff}.${ipInt shr 8 and 0xff}.${ipInt shr 16 and 0xff}.${ipInt shr 24 and 0xff}"
            }
        }
        return try {
            NetworkInterface.getNetworkInterfaces()?.let { interfaces ->
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback || !iface.isUp) continue
                    val addresses = iface.inetAddresses ?: continue
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            return addr.hostAddress ?: "Unknown"
                        }
                    }
                }
            }
            "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }
}
