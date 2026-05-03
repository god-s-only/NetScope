package com.netscope.app.data.proxy.cert

import android.content.Context
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import timber.log.Timber
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CertificateManager @Inject constructor(
    private val context: Context,
) {
    companion object {
        private const val CA_ALIAS       = "netscope_ca"
        private const val KEYSTORE_FILE  = "netscope_keystore.bks"
        private const val KEYSTORE_PASS  = "netscope_ks_pass"
        private const val KEY_ALGORITHM  = "RSA"
        private const val SIGN_ALGORITHM = "SHA256withRSA"
        private const val KEY_SIZE       = 2048
        private val VALIDITY_MS          = 10L * 365 * 24 * 60 * 60 * 1000 // 10 years
    }

    private val hostCertCache = mutableMapOf<String, Pair<X509Certificate, PrivateKey>>()

    private var caCert: X509Certificate? = null
    private var caKey: PrivateKey? = null

    fun initialize() {
        try {
            val keystoreFile = context.getFileStreamPath(KEYSTORE_FILE)

            if (keystoreFile.exists()) {
                val ks = KeyStore.getInstance("BKS", "BC")
                context.openFileInput(KEYSTORE_FILE).use { stream ->
                    ks.load(stream, KEYSTORE_PASS.toCharArray())
                }
                caCert = ks.getCertificate(CA_ALIAS) as X509Certificate
                caKey  = (ks.getKey(CA_ALIAS, KEYSTORE_PASS.toCharArray()) as PrivateKey)
                Timber.d("CertificateManager: loaded existing CA cert")
            } else {
                val keyPair = generateKeyPair()
                caCert = generateCaCertificate(keyPair)
                caKey  = keyPair.private

                val ks = KeyStore.getInstance("BKS", "BC")
                ks.load(null, KEYSTORE_PASS.toCharArray())
                ks.setKeyEntry(
                    CA_ALIAS,
                    caKey,
                    KEYSTORE_PASS.toCharArray(),
                    arrayOf(caCert),
                )
                context.openFileOutput(KEYSTORE_FILE, Context.MODE_PRIVATE).use { stream ->
                    ks.store(stream, KEYSTORE_PASS.toCharArray())
                }
                Timber.d("CertificateManager: generated and saved new CA cert")
            }
        } catch (e: Exception) {
            Timber.e(e, "CertificateManager: failed to initialize")
            throw e
        }
    }

    fun getCaCertificateBytes(): ByteArray {
        return caCert?.encoded
            ?: throw IllegalStateException("CA not initialized")
    }

    fun getCertificateForHost(hostname: String): Pair<X509Certificate, PrivateKey> {
        hostCertCache[hostname]?.let { return it }

        val ca   = caCert ?: throw IllegalStateException("CA not initialized")
        val caKy = caKey  ?: throw IllegalStateException("CA not initialized")

        val keyPair = generateKeyPair()
        val now     = System.currentTimeMillis()

        val subject = X500Name("CN=$hostname, O=NetScope, OU=NetScope Proxy")
        val issuer  = X500Name("CN=NetScope CA, O=NetScope, OU=NetScope")

        val cert = JcaX509v3CertificateBuilder(
            issuer,
            BigInteger(64, SecureRandom()),
            Date(now - 24 * 60 * 60 * 1000),  // yesterday (clock skew tolerance)
            Date(now + VALIDITY_MS),
            subject,
            keyPair.public,
        )
            .addExtension(
                Extension.subjectAlternativeName,
                false,
                GeneralNames(GeneralName(GeneralName.dNSName, hostname)),
            )
            .build(JcaContentSignerBuilder(SIGN_ALGORITHM).build(caKy))

        val x509 = JcaX509CertificateConverter().getCertificate(cert)
        val pair = Pair(x509, keyPair.private)
        hostCertCache[hostname] = pair
        return pair
    }

    private fun generateKeyPair(): KeyPair {
        val gen = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        gen.initialize(KEY_SIZE, SecureRandom())
        return gen.generateKeyPair()
    }

    private fun generateCaCertificate(keyPair: KeyPair): X509Certificate {
        val now     = System.currentTimeMillis()
        val subject = X500Name("CN=NetScope CA, O=NetScope, OU=NetScope")

        val cert = JcaX509v3CertificateBuilder(
            subject,
            BigInteger(64, SecureRandom()),
            Date(now - 24 * 60 * 60 * 1000),
            Date(now + VALIDITY_MS),
            subject,
            keyPair.public,
        )
            .addExtension(
                Extension.basicConstraints,
                true,
                BasicConstraints(true), // is a CA
            )
            .build(JcaContentSignerBuilder(SIGN_ALGORITHM).build(keyPair.private))

        return JcaX509CertificateConverter().getCertificate(cert)
    }
}