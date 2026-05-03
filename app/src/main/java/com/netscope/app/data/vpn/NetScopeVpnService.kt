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
import com.netscope.app.domain.model.PacketInfo
import com.netscope.app.domain.model.Protocol
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@AndroidEntryPoint
class NetScopeVpnService : VpnService() {

    @Inject lateinit var packetEventBus: PacketEventBus
    @Inject lateinit var uidResolver: UidResolver

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_START    = "com.netscope.vpn.START"
        const val ACTION_STOP     = "com.netscope.vpn.STOP"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID      = "netscope_vpn"
        private const val BUFFER_SIZE     = 32767

        // ports we never want to log — system noise
        private val IGNORED_PORTS = setOf(
            123,  // NTP — system clock sync, spams constantly
            5353, // mDNS — local network discovery
            1900, // SSDP — UPnP discovery
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> { startVpn(); START_STICKY }
            ACTION_STOP  -> { stopVpn();  START_NOT_STICKY }
            else         -> START_NOT_STICKY
        }
    }

    private fun startVpn() {
        try {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, buildNotification())
            vpnInterface = buildVpnInterface()
            startPacketCapture()
            Timber.i("NetScope VPN started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start VPN")
            stopSelf()
        }
    }

    private fun buildVpnInterface(): ParcelFileDescriptor {
        return Builder()
            .setSession("NetScope")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("8.8.8.8")
            .addDnsServer("8.8.4.4")
            .setMtu(BUFFER_SIZE)
            // CRITICAL: exclude our own app from the tunnel
            // Without this, our forwarding traffic re-enters the tunnel
            // causing an infinite loop and killing internet connectivity
            .addDisallowedApplication(packageName)
            .establish()
            ?: throw IllegalStateException("VPN interface could not be established")
    }

    private fun startPacketCapture() {
        val fd = vpnInterface ?: return

        serviceJob = serviceScope.launch {
            val inputStream  = FileInputStream(fd.fileDescriptor)
            val outputStream = FileOutputStream(fd.fileDescriptor)
            val rawBuffer    = ByteArray(BUFFER_SIZE)

            Timber.d("Packet capture loop started")

            while (isActive) {
                try {
                    val length = inputStream.read(rawBuffer)
                    if (length <= 0) continue

                    // STEP 1: forward immediately — never block internet
                    outputStream.write(rawBuffer, 0, length)

                    // STEP 2: copy bytes before launching coroutine
                    // rawBuffer will be overwritten on next iteration
                    val packetCopy = rawBuffer.copyOf(length)

                    // STEP 3: parse and emit without blocking the loop
                    launch {
                        processPacket(packetCopy, length)
                    }

                } catch (e: CancellationException) {
                    Timber.d("Capture loop cancelled")
                    break
                } catch (e: Exception) {
                    Timber.w(e, "Error in capture loop")
                }
            }
            Timber.d("Packet capture loop ended")
        }
    }

    private suspend fun processPacket(packetBytes: ByteArray, length: Int) {
        try {
            val buffer = ByteBuffer.wrap(packetBytes)
            val raw    = PacketParser.parse(buffer, length) ?: return

            // filter system noise — NTP, mDNS, SSDP etc.
            if (raw.destinationPort in IGNORED_PORTS ||
                raw.sourcePort      in IGNORED_PORTS) {
                return
            }

            // filter ICMP — no ports, not useful to show
            if (raw.protocol == Protocol.ICMP) return

            // handle DNS (port 53 only — not the fake DNS retries)
            if (raw.destinationPort == PacketParser.DNS_PORT ||
                raw.sourcePort      == PacketParser.DNS_PORT) {
                handleDnsPacket(raw)
            }

            // resolve UID best-effort — will be -1 on API 29+
            // for most apps due to /proc/net restrictions
            val uid     = uidResolver.resolveUid(raw.sourcePort, raw.protocol) ?: -1
            val appInfo = if (uid > 0) uidResolver.resolveAppInfo(uid) else null

            val packetInfo = PacketInfo(
                id              = UUID.randomUUID().toString(),
                timestampMs     = raw.timestampMs,
                protocol        = raw.protocol,
                sourceIp        = raw.sourceIp,
                destinationIp   = raw.destinationIp,
                sourcePort      = raw.sourcePort,
                destinationPort = raw.destinationPort,
                sizeBytes       = raw.sizeBytes,
                direction       = raw.direction,
                uid             = uid,
                appInfo         = appInfo,
            )

            packetEventBus.emitPacket(packetInfo)

        } catch (e: Exception) {
            Timber.w(e, "Failed to process packet")
        }
    }

    private suspend fun handleDnsPacket(raw: RawPacket) {
        if (raw.payload.isEmpty()) return
        try {
            val query    = DnsPacketParser.parseQuery(raw.payload)
            val response = DnsPacketParser.parseResponse(raw.payload)
            if (query != null || response != null) {
                packetEventBus.emitDnsEvent(
                    DnsEvent(
                        query    = query,
                        response = response,
                        uid      = -1, // UID not reliable on API 29+
                    )
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to handle DNS packet")
        }
    }

    private fun stopVpn() {
        Timber.i("NetScope VPN stopping")
        serviceJob?.cancel()
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        vpnInterface?.close()
        Timber.d("NetScopeVpnService destroyed")
    }

    override fun onRevoke() {
        super.onRevoke()
        stopVpn()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "NetScope VPN",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "NetScope traffic capture is active"
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
            .setContentText("Capturing device network traffic")
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