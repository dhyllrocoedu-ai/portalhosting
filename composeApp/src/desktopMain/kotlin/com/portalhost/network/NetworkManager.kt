package com.portalhost.network

import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

data class NetworkInfo(
    val localIp: String = "Unknown",
    val isConnected: Boolean = false
)

class NetworkManager {
    fun getLocalIpAddress(): NetworkInfo {
        try {
            val socket = DatagramSocket()
            socket.connect(InetAddress.getByName("8.8.8.8"), 80)
            val localAddr = socket.localAddress
            socket.close()
            if (localAddr is Inet4Address) {
                return NetworkInfo(localIp = localAddr.hostAddress ?: "Unknown", isConnected = true)
            }
        } catch (_: Exception) {}

        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.asSequence() ?: emptySequence()
            for (iface in interfaces) {
                if (iface.isLoopback || !iface.isUp) continue
                for (addr in iface.inetAddresses) {
                    if (addr is Inet4Address) {
                        return NetworkInfo(localIp = addr.hostAddress ?: "Unknown", isConnected = true)
                    }
                }
            }
        } catch (_: Exception) {}

        return NetworkInfo()
    }
}
