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

    @Inject
    lateinit var packetEventBus: PacketEventBus
    @Inject lateinit var uidResolver: UidResolver

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_START = "com.netscope.vpn.START"
        const val ACTION_STOP  = "com.netscope.vpn.STOP"

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "netscope_vpn"
        private const val BUFFER_SIZE = 32767
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> {
                startVpn()
                START_STICKY
            }
            ACTION_STOP -> {
                stopVpn()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
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
            .establish()
            ?: throw IllegalStateException("VPN interface could not be established")
    }

    private fun startPacketCapture() {
        val fd = vpnInterface ?: return

        serviceJob = serviceScope.launch {
            val inputStream = FileInputStream(fd.fileDescriptor)
            val outputStream = FileOutputStream(fd.fileDescriptor)
            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val rawBuffer = ByteArray(BUFFER_SIZE)

            Timber.d("Packet capture loop started")

            while (isActive) {
                try {
                    val length = inputStream.read(rawBuffer)
                    if (length <= 0) continue

                    buffer.clear()
                    buffer.put(rawBuffer, 0, length)
                    buffer.rewind()

                    val raw = PacketParser.parse(buffer, length) ?: continue

                    launch {
                        processPacket(raw, outputStream, rawBuffer, length)
                    }

                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Timber.w(e, "Error in packet capture loop")
                }
            }

            Timber.d("Packet capture loop ended")
        }
    }

    private suspend fun processPacket(
        raw: RawPacket,
        outputStream: FileOutputStream,
        rawBuffer: ByteArray,
        length: Int,
    ) {
        val appInfo = uidResolver.resolve(raw.sourcePort, raw.protocol)
        val uid = uidResolver.resolveUid(raw.sourcePort, raw.protocol) ?: -1

        if (raw.destinationPort == PacketParser.DNS_PORT ||
            raw.sourcePort == PacketParser.DNS_PORT) {
            handleDnsPacket(raw, uid, appInfo?.uid ?: uid)
        }

        val packetInfo = PacketInfo(
            id = UUID.randomUUID().toString(),
            timestampMs = raw.timestampMs,
            protocol = raw.protocol,
            sourceIp = raw.sourceIp,
            destinationIp = raw.destinationIp,
            sourcePort = raw.sourcePort,
            destinationPort = raw.destinationPort,
            sizeBytes = raw.sizeBytes,
            direction = raw.direction,
            uid = uid,
            appInfo = appInfo,
        )

        packetEventBus.emitPacket(packetInfo)

        withContext(Dispatchers.IO) {
            try {
                outputStream.write(rawBuffer, 0, length)
            } catch (e: Exception) {
                Timber.w(e, "Failed to forward packet")
            }
        }
    }

    private suspend fun handleDnsPacket(raw: RawPacket, uid: Int, resolvedUid: Int) {
        val payload = raw.payload
        if (payload.isEmpty()) return

        val query    = DnsPacketParser.parseQuery(payload)
        val response = DnsPacketParser.parseResponse(payload)

        if (query != null || response != null) {
            packetEventBus.emitDnsEvent(
                DnsEvent(
                    query = query,
                    response = response,
                    uid = resolvedUid,
                )
            )
        }
    }

    private fun stopVpn() {
        serviceJob?.cancel()
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Timber.i("NetScope VPN stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob?.cancel()
        serviceScope.cancel()
        vpnInterface?.close()
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
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
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
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}