package com.netscope.app.data.proxy

import com.netscope.app.data.proxy.cert.CertificateManager
import com.netscope.app.domain.model.HttpMethod
import com.netscope.app.domain.model.HttpTransaction
import com.netscope.app.domain.model.StatusCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

@Singleton
class LocalProxyServer @Inject constructor(
    private val certificateManager: CertificateManager,
    private val transactionEmitter: HttpTransactionEmitter,
) {
    companion object {
        const val PROXY_PORT   = 8888
        private const val BUFFER_SIZE = 8192
    }

    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        scope.launch {
            try {
                serverSocket = ServerSocket(PROXY_PORT)
                Timber.d("LocalProxyServer started on port $PROXY_PORT")

                while (true) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch {
                        handleConnection(clientSocket)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "LocalProxyServer error")
            }
        }
    }

    fun stop() {
        serverSocket?.close()
        serverSocket = null
        Timber.d("LocalProxyServer stopped")
    }

    private suspend fun handleConnection(clientSocket: Socket) {
        try {
            val input  = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()

            val reader      = BufferedReader(InputStreamReader(input))
            val requestLine = reader.readLine() ?: return

            Timber.d("Proxy received: $requestLine")

            val parts  = requestLine.trim().split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val target = parts[1]

            if (method.equals("CONNECT", ignoreCase = true)) {
                handleHttpsConnect(
                    clientSocket = clientSocket,
                    target       = target,
                    reader       = reader,
                    output       = output,
                )
            } else {
                handleHttp(
                    clientSocket = clientSocket,
                    method       = method,
                    target       = target,
                    reader       = reader,
                    output       = output,
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "handleConnection error")
        } finally {
            runCatching { clientSocket.close() }
        }
    }


    private suspend fun handleHttpsConnect(
        clientSocket: Socket,
        target: String,
        reader: BufferedReader,
        output: OutputStream,
    ) {
        val (hostname, port) = parseHostPort(target, 443)

        consumeHeaders(reader)

        output.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
        output.flush()

        val (hostCert, hostKey) = certificateManager.getCertificateForHost(hostname)

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        val ks  = java.security.KeyStore.getInstance("JKS")
        ks.load(null, null)
        ks.setKeyEntry("host", hostKey, "".toCharArray(), arrayOf(hostCert))
        kmf.init(ks, "".toCharArray())

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, null, null)

        val sslSocket = sslContext.socketFactory
            .createSocket(clientSocket, hostname, port, true) as SSLSocket
        sslSocket.useClientMode = false

        try {
            sslSocket.startHandshake()
        } catch (e: Exception) {
            Timber.w(e, "TLS handshake failed for $hostname — cert not trusted or pinned")
            sslSocket.close()
            return
        }
        val sslReader = BufferedReader(InputStreamReader(sslSocket.inputStream))
        val sslRequestLine = sslReader.readLine() ?: return

        val parts  = sslRequestLine.trim().split(" ")
        if (parts.size < 2) return

        val method = parts[0]
        val path   = parts[1]

        handleHttp(
            clientSocket = sslSocket,
            method       = method,
            target       = "https://$hostname$path",
            reader       = sslReader,
            output       = sslSocket.outputStream,
            hostname     = hostname,
            port         = port,
            isHttps      = true,
        )
    }


    private suspend fun handleHttp(
        clientSocket: Socket,
        method: String,
        target: String,
        reader: BufferedReader,
        output: OutputStream,
        hostname: String? = null,
        port: Int = 80,
        isHttps: Boolean = false,
    ) {
        val startMs = System.currentTimeMillis()
        val id      = UUID.randomUUID().toString()

        val requestHeaders = mutableMapOf<String, String>()
        var contentLength  = 0
        var line           = reader.readLine()

        while (!line.isNullOrBlank()) {
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val key   = line.substring(0, colonIdx).trim()
                val value = line.substring(colonIdx + 1).trim()
                requestHeaders[key] = value
                if (key.equals("Content-Length", ignoreCase = true)) {
                    contentLength = value.toIntOrNull() ?: 0
                }
            }
            line = reader.readLine()
        }

        val requestBody = if (contentLength > 0) {
            val bodyChars = CharArray(contentLength)
            reader.read(bodyChars, 0, contentLength)
            String(bodyChars)
        } else null

        val actualHost = hostname
            ?: requestHeaders["Host"]
            ?: parseHostFromUrl(target)
            ?: return

        val actualPort = if (isHttps) port else 80
        val path       = if (isHttps) {
            target.substringAfter("://").substringAfter("/", "").let { "/$it" }
        } else {
            target
        }

        val fullUrl = if (isHttps) target else "http://$actualHost$path"

        Timber.d("Proxy → $method $fullUrl")

        try {
            val serverSocket = if (isHttps) {
                createProtectedSslSocket(actualHost, actualPort)
            } else {
                createProtectedSocket(actualHost, actualPort)
            }

            serverSocket.use { socket ->
                val serverOut = socket.getOutputStream()
                val serverIn  = socket.getInputStream()

                val requestBuilder = StringBuilder()
                requestBuilder.append("$method $path HTTP/1.1\r\n")
                requestHeaders.forEach { (k, v) ->
                    if (!k.equals("Connection", ignoreCase = true) &&
                        !k.equals("Proxy-Connection", ignoreCase = true)) {
                        requestBuilder.append("$k: $v\r\n")
                    }
                }
                requestBuilder.append("Connection: close\r\n")
                requestBuilder.append("\r\n")
                requestBody?.let { requestBuilder.append(it) }

                serverOut.write(requestBuilder.toString().toByteArray())
                serverOut.flush()

                val responseReader = BufferedReader(InputStreamReader(serverIn))
                val statusLine     = responseReader.readLine() ?: return@use

                val statusParts   = statusLine.split(" ")
                val statusCode    = statusParts.getOrNull(1)?.toIntOrNull() ?: 0
                val statusMessage = statusParts.drop(2).joinToString(" ")

                val responseHeaders    = mutableMapOf<String, String>()
                var responseContentLen = -1
                var transferEncoding   = ""

                var rLine = responseReader.readLine()
                while (!rLine.isNullOrBlank()) {
                    val colonIdx = rLine.indexOf(':')
                    if (colonIdx > 0) {
                        val k = rLine.substring(0, colonIdx).trim()
                        val v = rLine.substring(colonIdx + 1).trim()
                        responseHeaders[k] = v
                        when {
                            k.equals("Content-Length", ignoreCase = true) ->
                                responseContentLen = v.toIntOrNull() ?: -1
                            k.equals("Transfer-Encoding", ignoreCase = true) ->
                                transferEncoding = v
                        }
                    }
                    rLine = responseReader.readLine()
                }

                // read response body
                val responseBody = readResponseBody(
                    reader           = responseReader,
                    contentLength    = responseContentLen,
                    transferEncoding = transferEncoding,
                )

                val durationMs = System.currentTimeMillis() - startMs

                val responseBuilder = StringBuilder()
                responseBuilder.append("$statusLine\r\n")
                responseHeaders.forEach { (k, v) ->
                    if (!k.equals("Transfer-Encoding", ignoreCase = true)) {
                        responseBuilder.append("$k: $v\r\n")
                    }
                }
                responseBuilder.append("Content-Length: ${responseBody.length}\r\n")
                responseBuilder.append("Connection: close\r\n")
                responseBuilder.append("\r\n")
                responseBuilder.append(responseBody)

                output.write(responseBuilder.toString().toByteArray())
                output.flush()

                val transaction = HttpTransaction(
                    id                = id,
                    timestampMs       = startMs,
                    url               = fullUrl,
                    host              = actualHost,
                    path              = path,
                    method            = mapMethod(method),
                    requestHeaders    = requestHeaders,
                    requestBody       = requestBody,
                    requestSizeBytes  = requestBody?.length?.toLong() ?: 0L,
                    responseCode      = statusCode,
                    responseMessage   = statusMessage,
                    responseHeaders   = responseHeaders,
                    responseBody      = responseBody,
                    responseSizeBytes = responseBody.length.toLong(),
                    durationMs        = durationMs,
                    protocol          = if (isHttps) "HTTPS" else "HTTP",
                    isReplay          = false,
                    error             = null,
                )

                transactionEmitter.emit(transaction)
                Timber.d("Captured: $method $fullUrl → $statusCode (${durationMs}ms)")
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to forward request to $actualHost")

            val durationMs = System.currentTimeMillis() - startMs
            val transaction = HttpTransaction(
                id                = id,
                timestampMs       = startMs,
                url               = fullUrl,
                host              = actualHost,
                path              = path,
                method            = mapMethod(method),
                requestHeaders    = requestHeaders,
                requestBody       = requestBody,
                requestSizeBytes  = requestBody?.length?.toLong() ?: 0L,
                responseCode      = null,
                responseMessage   = null,
                responseHeaders   = emptyMap(),
                responseBody      = null,
                responseSizeBytes = 0L,
                durationMs        = durationMs,
                protocol          = if (isHttps) "HTTPS" else "HTTP",
                isReplay          = false,
                error             = e.message ?: "Connection failed",
            )
            transactionEmitter.emit(transaction)

            output.write("HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n".toByteArray())
            output.flush()
        }
    }


    private fun createProtectedSocket(host: String, port: Int): Socket {
        val socket = ProtectedSocketHolder.createProtectedSocket()
            ?: Socket() // fallback if VPN not active
        socket.connect(java.net.InetSocketAddress(host, port), 10_000)
        return socket
    }

    private fun createProtectedSslSocket(host: String, port: Int): Socket {
        val plainSocket = ProtectedSocketHolder.createProtectedSocket()
            ?: Socket()
        plainSocket.connect(java.net.InetSocketAddress(host, port), 10_000)

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, null, null)

        return (sslContext.socketFactory as SSLSocketFactory)
            .createSocket(plainSocket, host, port, true) as SSLSocket
    }

    private fun readResponseBody(
        reader: BufferedReader,
        contentLength: Int,
        transferEncoding: String,
    ): String {
        return try {
            if (transferEncoding.equals("chunked", ignoreCase = true)) {
                readChunkedBody(reader)
            } else if (contentLength > 0) {
                val chars = CharArray(contentLength)
                reader.read(chars, 0, contentLength)
                String(chars)
            } else {
                reader.readText()
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to read response body")
            ""
        }
    }

    private fun readChunkedBody(reader: BufferedReader): String {
        val sb = StringBuilder()
        while (true) {
            val sizeLine  = reader.readLine()?.trim() ?: break
            val chunkSize = sizeLine.toIntOrNull(16) ?: break
            if (chunkSize == 0) break
            val chars = CharArray(chunkSize)
            reader.read(chars, 0, chunkSize)
            sb.append(chars)
            reader.readLine()
        }
        return sb.toString()
    }

    private fun consumeHeaders(reader: BufferedReader) {
        var line = reader.readLine()
        while (!line.isNullOrBlank()) {
            line = reader.readLine()
        }
    }

    private fun parseHostPort(target: String, defaultPort: Int): Pair<String, Int> {
        val parts = target.split(":")
        return if (parts.size == 2) {
            Pair(parts[0], parts[1].toIntOrNull() ?: defaultPort)
        } else {
            Pair(target, defaultPort)
        }
    }

    private fun parseHostFromUrl(url: String): String? {
        return try {
            val withScheme = if (!url.startsWith("http")) "http://$url" else url
            java.net.URL(withScheme).host
        } catch (e: Exception) { null }
    }

    private fun mapMethod(method: String): HttpMethod = when (method.uppercase()) {
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