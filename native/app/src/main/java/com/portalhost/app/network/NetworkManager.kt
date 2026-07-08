package com.portalhost.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

data class NetworkInfo(
    val localIp: String = "Unknown",
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

    private val _networkInfo = MutableStateFlow(getNetworkInfo())
    val networkInfo: StateFlow<NetworkInfo> = _networkInfo.asStateFlow()

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) { _networkInfo.value = getNetworkInfo() }
        override fun onLost(network: Network) { _networkInfo.value = getNetworkInfo() }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { _networkInfo.value = getNetworkInfo() }
    }

    init {
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register network callback: ${e.message}")
        }
        _networkInfo.value = getNetworkInfo()
    }

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
        // 0. DatagramSocket — no permissions needed, works on modern Android.
        //    Establishes a temporary UDP connection to determine which local
        //    interface the kernel routes through. No actual packets are sent.
        try {
            val socket = DatagramSocket()
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002)
            val localAddr = socket.localAddress
            socket.close()
            if (localAddr is Inet4Address && !localAddr.isLoopbackAddress) {
                val ip = localAddr.hostAddress ?: ""
                if (ip.isNotBlank()) return ip
            }
        } catch (_: Exception) {}

        // 1. Direct WiFi info (fast on older Android, deprecated on API 31+)
        val wifiInfo = wifiManager?.connectionInfo
        if (wifiInfo != null) {
            val ipInt = wifiInfo.ipAddress
            if (ipInt != 0) {
                return "${ipInt and 0xff}.${ipInt shr 8 and 0xff}.${ipInt shr 16 and 0xff}.${ipInt shr 24 and 0xff}"
            }
        }

        return try {
            val activeNetwork = connectivityManager.activeNetwork
            val linkProps = activeNetwork?.let { connectivityManager.getLinkProperties(it) }
            val preferredIface = linkProps?.interfaceName

            // 2. Check LinkAddresses directly from ConnectivityManager
            linkProps?.linkAddresses?.forEach { addr ->
                val inet = addr.address ?: return@forEach
                if (inet is Inet4Address && !inet.isLoopbackAddress) {
                    return inet.hostAddress ?: ""
                }
            }

            // 3. Enumerate all interfaces — try known names first, then fall through to any IPv4
            val knownInterfaces = listOf(
                "wlan0", "eth0", "rmnet0", "ccmni0", "ccmni1", "ccmni2", "ccmni3",
                "r_rmnet_data0", "r_rmnet_data1", "rmnet_data0", "rmnet_data1",
                "rmnet_data2", "rmnet_data3", "rmnet_data4", "rmnet_data5",
                "rmnet_data6", "rmnet_data7", "rmnet_data8", "ril0", "usb0",
                "ap0", "p2p0", "bond0", "dummy0", "ifb0", "teql0", "sit0",
                "tun0", "tap0", "softap0", "wlan1"
            )

            // Track best non-known candidate
            var bestIp: String? = null

            NetworkInterface.getNetworkInterfaces()?.let { interfaces ->
                while (interfaces.hasMoreElements()) {
                    val iface = interfaces.nextElement()
                    if (iface.isLoopback) continue
                    val addresses = iface.inetAddresses ?: continue
                    var ipv4: String? = null
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            ipv4 = addr.hostAddress ?: continue
                            break
                        }
                    }
                    if (ipv4 == null) continue

                    if (iface.name == preferredIface && iface.name in knownInterfaces) {
                        return ipv4
                    }
                    if (iface.name in knownInterfaces) {
                        bestIp = ipv4
                        continue
                    }
                    if (iface.isUp && bestIp == null) {
                        bestIp = ipv4
                    }
                }
            }

            if (bestIp != null) return bestIp!!

            "Unknown"
        } catch (_: Exception) {
            "Unknown"
        }
    }
}
