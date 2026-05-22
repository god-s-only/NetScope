package com.netscope.app.data.proxy

import android.util.Log
import com.netscope.app.data.proxy.cert.CertificateManager
import com.netscope.app.domain.model.HttpMethod
import com.netscope.app.domain.model.HttpTransaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory

private const val TAG = "LocalProxyServer"

@Singleton
class LocalProxyServer @Inject constructor(
    private val certificateManager: CertificateManager,
    private val transactionEmitter: HttpTransactionEmitter,
) {
    companion object {
        const val PORT = 8888
        private const val MAX_BODY_BYTES = 500_000
    }

    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    fun start() {
        if (isRunning) {
            Log.d(TAG, "Already running")
            return
        }
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                isRunning = true
                Log.d(TAG, "Started on port $PORT")
                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    launch { handleClient(client) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error: ${e.message}")
                isRunning = false
            }
        }
    }

    fun stop() {
        isRunning = false
        serverSocket?.close()
        serverSocket = null
        Log.d(TAG, "Stopped")
    }

    fun isRunning() = isRunning


    private suspend fun handleClient(client: Socket) {
        try {
            client.soTimeout = 30_000
            val input  = client.getInputStream()
            val output = client.getOutputStream()
            val reader = input.bufferedReader()

            val requestLine = reader.readLine()?.trim() ?: return
            Log.d(TAG, "Request: $requestLine")

            val parts = requestLine.split(" ")
            if (parts.size < 3) return

            val method = parts[0].uppercase()
            val target = parts[1]

            if (method == "CONNECT") {
                handleConnect(client, target, reader, output)
            } else {
                handlePlainHttp(method, target, reader, output)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Client error: ${e.message}")
        } finally {
            runCatching { client.close() }
        }
    }


    private suspend fun handleConnect(
        client: Socket,
        target: String,
        reader: BufferedReader,
        output: OutputStream,
    ) {
        var line = reader.readLine()
        while (!line.isNullOrBlank()) {
            line = reader.readLine()
        }

        val (host, port) = splitHostPort(target, 443)

        output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
        output.flush()

        try {
            val sslSocket = upgradeTls(client, host, port) ?: return
            Log.d(TAG, "TLS upgrade success for $host")

            sslSocket.use {
                val sslReader = it.inputStream.bufferedReader()
                val sslOutput = it.outputStream

                val firstLine = sslReader.readLine()?.trim() ?: return
                val sslParts = firstLine.split(" ")
                if (sslParts.size < 2) return

                val method = sslParts[0].uppercase()
                val path = sslParts[1]

                captureAndForward(
                    method = method,
                    targetUrl = "https://$host$path",
                    hostHint = host,
                    portHint = port,
                    pathHint = path,
                    isHttps = true,
                    reader = sslReader,
                    clientOutput = sslOutput,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "CONNECT failed for $host: ${e.message}")
        }
    }


    private suspend fun handlePlainHttp(
        method: String,
        target: String,
        reader: BufferedReader,
        clientOutput: OutputStream,
    ) {
        val isAbsolute = target.startsWith("http://", ignoreCase = true)

        val url = if (isAbsolute) target else null
        val hostFromUrl = if (isAbsolute) extractHost(target) else null
        val portFromUrl = if (isAbsolute) extractPort(target, 80) else 80
        val pathFromUrl = if (isAbsolute) extractPath(target) else target

        captureAndForward(
            method = method,
            targetUrl = url,
            hostHint = hostFromUrl,
            portHint = portFromUrl,
            pathHint = pathFromUrl,
            isHttps = false,
            reader = reader,
            clientOutput = clientOutput,
        )
    }


    private suspend fun captureAndForward(
        method: String,
        targetUrl: String?,
        hostHint: String?,
        portHint: Int,
        pathHint: String,
        isHttps: Boolean,
        reader: BufferedReader,
        clientOutput: OutputStream,
        hostnameForTls: String? = null,
    ) {
        val startMs = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        val reqHeaders = mutableMapOf<String, String>()
        var contentLength = 0

        var line = reader.readLine()
        while (!line.isNullOrBlank()) {
            val colon = line.indexOf(':')
            if (colon > 0) {
                val k = line.substring(0, colon).trim()
                val v = line.substring(colon + 1).trim()
                reqHeaders[k] = v
                if (k.equals("Content-Length", ignoreCase = true)) {
                    contentLength = v.toIntOrNull() ?: 0
                }
            }
            line = reader.readLine()
        }

        val hostHeader = reqHeaders["Host"]?.trim() ?: ""
        val host = when {
            !hostHint.isNullOrBlank() -> hostHint
            hostHeader.contains(":") -> hostHeader.substringBefore(":")
            hostHeader.isNotBlank() -> hostHeader
            else -> {
                Log.e(TAG, "Cannot determine host for request $method $pathHint")
                clientOutput.write(
                    "HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n".toByteArray()
                )
                return
            }
        }

        val port = when {
            portHint != 80 || isHttps -> portHint
            hostHeader.contains(":") ->
                hostHeader.substringAfter(":").toIntOrNull() ?: portHint
            else -> portHint
        }

        val path = pathHint
        val url = targetUrl ?: if (isHttps) "https://$host$path" else "http://$host$path"

        val reqBody = if (contentLength > 0) {
            val buf = CharArray(contentLength.coerceAtMost(MAX_BODY_BYTES))
            reader.read(buf)
            String(buf)
        } else null

        Log.d(TAG, "Forwarding: $method $url → $host:$port")

        try {
            val serverSocket = Socket(host, port)
            serverSocket.soTimeout = 30_000

            val serverOut: OutputStream
            val serverIn: InputStream

            if (isHttps) {
                val ssl = SSLContext.getInstance("TLS")
                ssl.init(null, null, null)
                val sslSock = ssl.socketFactory.createSocket(
                    serverSocket, host, port, true
                ) as SSLSocket
                sslSock.startHandshake()
                serverOut = sslSock.outputStream
                serverIn = sslSock.inputStream
            } else {
                serverOut = serverSocket.outputStream
                serverIn = serverSocket.inputStream
            }

            val reqBuilder = StringBuilder()
            reqBuilder.append("$method $path HTTP/1.1\r\n")
            reqHeaders.forEach { (k, v) ->
                if (!k.equals("Proxy-Connection", ignoreCase = true) &&
                    !k.equals("Connection", ignoreCase = true)) {
                    reqBuilder.append("$k: $v\r\n")
                }
            }
            reqBuilder.append("Connection: close\r\n\r\n")
            reqBody?.let { reqBuilder.append(it) }
            serverOut.write(reqBuilder.toString().toByteArray())
            serverOut.flush()

            val serverReader = serverIn.bufferedReader()
            val statusLine = serverReader.readLine()?.trim() ?: return
            val statusParts = statusLine.split(" ", limit = 3)
            val statusCode = statusParts.getOrNull(1)?.toIntOrNull() ?: 0
            val statusMessage = statusParts.getOrNull(2) ?: ""

            val respHeaders = mutableMapOf<String, String>()
            var respContentLen = -1
            var transferEncoding = ""

            var rLine = serverReader.readLine()
            while (!rLine.isNullOrBlank()) {
                val colon = rLine.indexOf(':')
                if (colon > 0) {
                    val k = rLine.substring(0, colon).trim()
                    val v = rLine.substring(colon + 1).trim()
                    respHeaders[k] = v
                    when {
                        k.equals("Content-Length", ignoreCase = true) ->
                            respContentLen = v.toIntOrNull() ?: -1
                        k.equals("Transfer-Encoding", ignoreCase = true) ->
                            transferEncoding = v
                    }
                }
                rLine = serverReader.readLine()
            }

            val respBody = readBody(serverReader, respContentLen, transferEncoding)
            val durationMs = System.currentTimeMillis() - startMs

            val respBuilder = StringBuilder()
            respBuilder.append("$statusLine\r\n")
            respHeaders.forEach { (k, v) ->
                if (!k.equals("Transfer-Encoding", ignoreCase = true)) {
                    respBuilder.append("$k: $v\r\n")
                }
            }
            respBuilder.append("Content-Length: ${respBody.toByteArray().size}\r\n")
            respBuilder.append("Connection: close\r\n\r\n")
            respBuilder.append(respBody)
            clientOutput.write(respBuilder.toString().toByteArray())
            clientOutput.flush()

            runCatching { serverSocket.close() }

            transactionEmitter.emit(
                HttpTransaction(
                    id = id,
                    timestampMs = startMs,
                    url = url,
                    host = host,
                    path = path,
                    method = parseMethod(method),
                    requestHeaders = reqHeaders,
                    requestBody = reqBody,
                    requestSizeBytes = reqBody?.toByteArray()?.size?.toLong() ?: 0L,
                    responseCode = statusCode,
                    responseMessage = statusMessage,
                    responseHeaders = respHeaders,
                    responseBody = respBody,
                    responseSizeBytes = respBody.toByteArray().size.toLong(),
                    durationMs = durationMs,
                    protocol = if (isHttps) "HTTPS" else "HTTP",
                    isReplay = false,
                    error = null,
                )
            )

            Log.d(TAG, "Captured: $method $url → $statusCode (${durationMs}ms)")

        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startMs
            Log.e(TAG, "captureAndForward failed for $host:$port — " +
                    "${e.javaClass.simpleName}: ${e.message}")

            transactionEmitter.emit(
                HttpTransaction(
                    id = id,
                    timestampMs = startMs,
                    url = url,
                    host = host,
                    path = path,
                    method = parseMethod(method),
                    requestHeaders = reqHeaders,
                    requestBody = reqBody,
                    requestSizeBytes = reqBody?.toByteArray()?.size?.toLong() ?: 0L,
                    responseCode = null,
                    responseMessage = null,
                    responseHeaders = emptyMap(),
                    responseBody = null,
                    responseSizeBytes = 0L,
                    durationMs = durationMs,
                    protocol = if (isHttps) "HTTPS" else "HTTP",
                    isReplay = false,
                    error = e.message ?: "Connection failed",
                )
            )

            runCatching {
                clientOutput.write(
                    "HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n".toByteArray()
                )
                clientOutput.flush()
            }
        }
    }

    private fun upgradeTls(client: Socket, host: String, port: Int): SSLSocket? {
        return try {
            val (cert, key) = certificateManager.getCertificateForHost(host)

            val ks = KeyStore.getInstance(KeyStore.getDefaultType())
            ks.load(null, null)
            ks.setKeyEntry("key", key, "".toCharArray(), arrayOf(cert))

            val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            kmf.init(ks, "".toCharArray())

            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(null as KeyStore?)

            val sslCtx = SSLContext.getInstance("TLS")
            sslCtx.init(kmf.keyManagers, tmf.trustManagers, null)

            val sslSocket = sslCtx.socketFactory
                .createSocket(client, host, port, true) as SSLSocket
            sslSocket.useClientMode = false
            sslSocket.startHandshake()
            sslSocket
        } catch (e: Exception) {
            Log.w(TAG, "upgradeTls failed for $host: ${e.message}")
            null
        }
    }

    private fun readBody(
        reader: BufferedReader,
        contentLength: Int,
        transferEncoding: String,
    ): String {
        return try {
            when {
                transferEncoding.equals("chunked", ignoreCase = true) -> {
                    val sb = StringBuilder()
                    while (true) {
                        val sizeLine  = reader.readLine()?.trim() ?: break
                        val chunkSize = sizeLine.toIntOrNull(16) ?: break
                        if (chunkSize == 0) break
                        val buf = CharArray(chunkSize.coerceAtMost(MAX_BODY_BYTES))
                        reader.read(buf)
                        sb.append(buf)
                        reader.readLine()
                    }
                    sb.toString()
                }
                contentLength > 0 -> {
                    val buf = CharArray(contentLength.coerceAtMost(MAX_BODY_BYTES))
                    reader.read(buf)
                    String(buf)
                }
                else -> reader.readText()
            }
        } catch (e: Exception) {
            Log.w(TAG, "readBody error: ${e.message}")
            ""
        }
    }


    private fun splitHostPort(target: String, default: Int): Pair<String, Int> {
        val parts = target.split(":")
        return Pair(parts[0], parts.getOrNull(1)?.toIntOrNull() ?: default)
    }

    private fun extractHost(url: String): String? = try {
        java.net.URL(url).host
    } catch (e: Exception) { null }

    private fun extractPort(url: String, default: Int): Int = try {
        val p = java.net.URL(url).port
        if (p == -1) default else p
    } catch (e: Exception) { default }

    private fun extractPath(url: String): String = try {
        val u = java.net.URL(url)
        val path = u.path.ifEmpty { "/" }
        if (u.query != null) "$path?${u.query}" else path
    } catch (e: Exception) { "/" }

    private fun parseMethod(method: String): HttpMethod = when (method) {
        "GET"     -> HttpMethod.GET
        "POST"    -> HttpMethod.POST
        "PUT"     -> HttpMethod.PUT
        "DELETE"  -> HttpMethod.DELETE
        "PATCH"   -> HttpMethod.PATCH
        "HEAD"    -> HttpMethod.HEAD
        "OPTIONS" -> HttpMethod.OPTIONS
        else      -> HttpMethod.UNKNOWN
    }
}