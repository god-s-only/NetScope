package com.netscope.app.data.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.netscope.app.MainActivity
import com.netscope.app.data.proxy.LocalProxyServer
import com.netscope.app.data.proxy.ProtectedSocketHolder
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.nio.ByteBuffer
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@AndroidEntryPoint
class NetScopeVpnService : VpnService() {

    @Inject lateinit var localProxyServer: LocalProxyServer

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_START = "com.netscope.app.vpn.START"
        const val ACTION_STOP = "com.netscope.app.vpn.STOP"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "netscope_vpn"
        private const val VPN_MTU = 16384
        private const val BUFFER_SIZE = 16384

        private const val HTTP_PORT = 80
        private const val HTTPS_PORT = 443
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> { start(); START_STICKY }
            ACTION_STOP -> { stop();  START_NOT_STICKY }
            else -> START_NOT_STICKY
        }
    }

    private fun start() {
        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, buildNotification())

            ProtectedSocketHolder.register(this)

            localProxyServer.start()

            vpnInterface = buildVpnInterface()

            startTunnel()

            Timber.i("NetScopeVpnService started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start VPN service")
            stop()
        }
    }

    private fun buildVpnInterface(): ParcelFileDescriptor {
        return Builder()
            .setSession("NetScope")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .setMtu(VPN_MTU)
            .addDisallowedApplication(packageName)
            .establish()
            ?: throw IllegalStateException("VPN interface could not be established")
    }

    private fun startTunnel() {
        tunnelJob = serviceScope.launch {
            val inputStream  = FileInputStream(vpnInterface!!.fileDescriptor)
            val outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)
            val buffer       = ByteArray(BUFFER_SIZE)

            Timber.d("Tunnel loop started")

            while (isActive) {
                try {
                    val length = inputStream.read(buffer)
                    if (length <= 0) continue

                    val packet = ByteArray(length)
                    System.arraycopy(buffer, 0, packet, 0, length)

                    val redirected = tryRedirectToProxy(packet, length)

                    outputStream.write(redirected, 0, redirected.size)

                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Timber.w(e, "Tunnel loop error")
                }
            }

            Timber.d("Tunnel loop ended")
        }
    }

    private fun tryRedirectToProxy(packet: ByteArray, length: Int): ByteArray {
        try {
            val bb = ByteBuffer.wrap(packet, 0, length)

            val firstByte = packet[0].toInt() and 0xFF
            val version   = firstByte shr 4
            if (version != 4) return packet

            val ipHeaderLen = (firstByte and 0x0F) * 4
            val protocol    = packet[9].toInt() and 0xFF
            if (protocol != 6) return packet

            val tcpOffset = ipHeaderLen
            val destPort  = ((packet[tcpOffset + 2].toInt() and 0xFF) shl 8) or
                    (packet[tcpOffset + 3].toInt() and 0xFF)

            if (destPort != HTTP_PORT && destPort != HTTPS_PORT) return packet

            val proxyIp = InetAddress.getByName("127.0.0.1").address
            packet[16] = proxyIp[0]
            packet[17] = proxyIp[1]
            packet[18] = proxyIp[2]
            packet[19] = proxyIp[3]

            packet[tcpOffset + 2] = ((LocalProxyServer.PROXY_PORT shr 8) and 0xFF).toByte()
            packet[tcpOffset + 3] = (LocalProxyServer.PROXY_PORT and 0xFF).toByte()

            recalculateIpChecksum(packet, ipHeaderLen)

            recalculateTcpChecksum(packet, ipHeaderLen, length)

            return packet

        } catch (e: Exception) {
            Timber.w(e, "tryRedirectToProxy failed — passing packet through")
            return packet
        }
    }

    private fun recalculateIpChecksum(packet: ByteArray, headerLen: Int) {
        packet[10] = 0
        packet[11] = 0

        var sum = 0
        var i   = 0
        while (i < headerLen) {
            val word = ((packet[i].toInt() and 0xFF) shl 8) or
                    (packet[i + 1].toInt() and 0xFF)
            sum += word
            i   += 2
        }
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        val checksum = sum.inv() and 0xFFFF
        packet[10] = (checksum shr 8).toByte()
        packet[11] = (checksum and 0xFF).toByte()
    }

    private fun recalculateTcpChecksum(packet: ByteArray, ipHeaderLen: Int, totalLen: Int) {
        val tcpLen = totalLen - ipHeaderLen

        packet[ipHeaderLen + 16] = 0
        packet[ipHeaderLen + 17] = 0

        var sum = 0

        for (i in 12..15) sum += (packet[i].toInt() and 0xFF) shl (if (i % 2 == 0) 8 else 0)
        for (i in 16..19) sum += (packet[i].toInt() and 0xFF) shl (if (i % 2 == 0) 8 else 0)
        sum += 6
        sum += tcpLen

        var i = ipHeaderLen
        while (i < totalLen - 1) {
            sum += ((packet[i].toInt() and 0xFF) shl 8) or (packet[i + 1].toInt() and 0xFF)
            i   += 2
        }
        if (totalLen % 2 != 0) {
            sum += (packet[totalLen - 1].toInt() and 0xFF) shl 8
        }

        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        val checksum = sum.inv() and 0xFFFF
        packet[ipHeaderLen + 16] = (checksum shr 8).toByte()
        packet[ipHeaderLen + 17] = (checksum and 0xFF).toByte()
    }

    private fun stop() {
        Timber.i("NetScopeVpnService stopping")
        tunnelJob?.cancel()
        vpnInterface?.close()
        vpnInterface = null
        localProxyServer.stop()
        ProtectedSocketHolder.unregister()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stop()
    }

    override fun onRevoke() {
        super.onRevoke()
        stop()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "NetScope VPN",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "NetScope is capturing network traffic"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, NetScopeVpnService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NetScope active")
            .setContentText("Capturing network traffic")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopIntent,
            )
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}