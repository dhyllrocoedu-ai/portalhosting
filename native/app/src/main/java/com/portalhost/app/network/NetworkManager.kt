package com.portalhost.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface

data class NetworkInfo(
    val localIp: String = "Unknown",
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

    fun getNetworkInfo(): NetworkInfo {
        return try {
            val network = connectivityManager.activeNetwork ?: return NetworkInfo()
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return NetworkInfo()
            val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val localIp = getLocalIpAddress()
            NetworkInfo(localIp, isWifi, isCellular, isConnected)
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing permission: ${e.message}")
            NetworkInfo()
        } catch (e: Exception) {
            Log.w(TAG, "Network check failed: ${e.message}")
            NetworkInfo()
        }
    }

    private fun getLocalIpAddress(): String {
        // Try WiFi first
        val wifiInfo = wifiManager?.connectionInfo
        if (wifiInfo != null) {
            val ipInt = wifiInfo.ipAddress
            if (ipInt != 0) {
                return "${ipInt and 0xff}.${ipInt shr 8 and 0xff}.${ipInt shr 16 and 0xff}.${ipInt shr 24 and 0xff}"
            }
        }

        // Fallback: iterate network interfaces
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
