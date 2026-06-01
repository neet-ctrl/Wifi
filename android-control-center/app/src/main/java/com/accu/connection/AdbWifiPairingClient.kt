package com.accu.connection

import android.util.Base64
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509TrustManager

/**
 * Implements Android Wireless ADB Pairing (SPAKE2 over TLS) in pure Kotlin.
 *
 * This is exactly what Shizuku does internally — no `adb` binary needed.
 * Works on any Android 11+ phone with wireless debugging enabled.
 *
 * Protocol references:
 *   AOSP: packages/modules/adb/pairing_auth/pairing_auth.cc
 *   AOSP: packages/modules/adb/pairing_connection/pairing_server.cpp
 *   BoringSSL: crypto/spake2/spake2.c
 */
object AdbWifiPairingClient {

    private const val TAG = "AdbWifiPairing"

    // ── P-256 (secp256r1) curve constants ─────────────────────────────────────
    private val P256P  = BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF", 16)
    private val P256A  = P256P - BigInteger.valueOf(3)            // a = -3
    private val P256N  = BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16)
    private val P256GX = BigInteger("6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296", 16)
    private val P256GY = BigInteger("4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5", 16)

    // SPAKE2 blinding generators for P-256 (from BoringSSL crypto/spake2/spake2.c)
    // M = client's generator (Alice role), N = server's generator (Bob role)
    private val SPAKE2_MX = BigInteger("886e2f97ace46e55ba9dd7242579f2993b64e16ef3dcab95afd497333d8fa12f", 16)
    private val SPAKE2_MY = BigInteger("5ff355163e43ce224e0b0e65ff02ac8e5c7be09419c785e0ca547d55a12e2d20", 16)
    private val SPAKE2_NX = BigInteger("d8bbd6c639c62937b04d997f38c3770719c629d7014d49a24b4f98baa1292b49", 16)
    private val SPAKE2_NY = BigInteger("07d60aa6bfade45008a636337f5168c64d9bd36034808cd564490b1cf3f847c2", 16)

    // ADB pairing context names — must match adbd exactly, INCLUDING null terminator
    private val CLIENT_NAME = "adb pair client\u0000".toByteArray(Charsets.UTF_8)  // 16 bytes
    private val SERVER_NAME = "adb pair server\u0000".toByteArray(Charsets.UTF_8)  // 16 bytes

    // HKDF info: "adb pairing_auth aes-256-gcm key" (from pairing_auth.cc)
    private val HKDF_INFO = "adb pairing_auth aes-256-gcm key".toByteArray(Charsets.UTF_8)

    private const val PKT_VERSION = 1
    private const val PKT_SPAKE2  = 0
    private const val PKT_CERT    = 1

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Perform SPAKE2 pairing with Android's wireless ADB pairing service.
     *
     * @param host         IP address discovered via mDNS (_adb-tls-pairing._tcp)
     * @param port         Pairing port discovered via mDNS
     * @param pairingCode  The 6-digit code shown in Settings → Developer options → Wireless debugging
     * @param pubKeyFile   The .pub file written by AdbKeyPair.generate() — contains our RSA public
     *                     key in ADB base64 format. The device authorises this key on success.
     * @return true if pairing succeeded and the device now trusts our key for future connections.
     */
    fun pair(host: String, port: Int, pairingCode: String, pubKeyFile: File): Boolean {
        return try {
            doPair(host, port, pairingCode, pubKeyFile)
        } catch (e: Exception) {
            Timber.e(e, "$TAG pair($host:$port) FAILED: ${e.message?.take(200)}")
            false
        }
    }

    // ── Internal implementation ───────────────────────────────────────────────

    private fun doPair(host: String, port: Int, pairingCode: String, pubKeyFile: File): Boolean {
        // ── 1. TLS connect (trust all — SPAKE2 provides authentication) ───────
        val sslCtx = SSLContext.getInstance("TLS")
        sslCtx.init(null, arrayOf(TrustAll), SecureRandom())
        val socket = sslCtx.socketFactory.createSocket(host, port) as SSLSocket
        socket.soTimeout = 15_000
        socket.startHandshake()
        Timber.d("$TAG TLS handshake done with $host:$port")

        val din  = DataInputStream(BufferedInputStream(socket.inputStream))
        val dout = DataOutputStream(BufferedOutputStream(socket.outputStream))

        return try {
            val password = pairingCode.toByteArray(Charsets.UTF_8)

            // ── 2. SPAKE2 client role (Alice) ──────────────────────────────────
            // Password scalar: w = SHA256(password) mod N
            val w = BigInteger(1, sha256(password)).mod(P256N)

            // Ephemeral secret x (random scalar)
            var x = BigInteger(256, SecureRandom()).mod(P256N)
            if (x.signum() == 0) x = BigInteger.ONE

            // Our blinded message: T = x*G + w*M  (what we send to server)
            val G = Pt(P256GX, P256GY)
            val M = Pt(SPAKE2_MX, SPAKE2_MY)
            val N = Pt(SPAKE2_NX, SPAKE2_NY)
            val T = ptAdd(ptMul(x, G), ptMul(w, M))
                ?: error("SPAKE2 client message is point at infinity")
            val ourMsg = ptEncode(T)

            // ── 3. Exchange SPAKE2 messages (server sends first) ───────────────
            val serverMsg = readPkt(din, PKT_SPAKE2)
            Timber.d("$TAG received server SPAKE2 msg (${serverMsg.size} bytes)")
            writePkt(dout, PKT_SPAKE2, ourMsg)
            Timber.d("$TAG sent client SPAKE2 msg (${ourMsg.size} bytes)")

            // ── 4. Compute shared EC point K = x * (S − w*N) ──────────────────
            val S  = ptDecode(serverMsg)
            val wN = ptMul(w, N) ?: error("w*N is infinity")
            val Y  = ptAdd(S, ptNeg(wN)) ?: error("unblinded server point is infinity")
            val K  = ptMul(x, Y) ?: error("shared point K is infinity")

            // ── 5. SPAKE2 key derivation — SHA-256 transcript (BoringSSL format)
            //   Hash( label || u32be|T|T || u32be|S|S || u32be|32|Kx
            //              || u32be|pwd|pwd || u32be|cn|cn || u32be|sn|sn )
            val Kx       = ptX32(K)
            val spake2Key = sha256Transcript(
                "SPAKE2 key\u0000".toByteArray(Charsets.UTF_8),
                ourMsg, serverMsg, Kx, password, CLIENT_NAME, SERVER_NAME
            )
            Timber.d("$TAG SPAKE2 key derived (${spake2Key.size} bytes)")

            // ── 6. HKDF: spake2Key → 44 bytes (AES-256 key + GCM nonce) ───────
            val km    = hkdf(spake2Key, HKDF_INFO, 44)
            val aesK  = SecretKeySpec(km, 0, 32, "AES")
            val nonce = km.copyOfRange(32, 44)

            // ── 7. Send our RSA public key encrypted with AES-256-GCM ──────────
            val pubKeyBytes = readAdbPubKey(pubKeyFile)
            val enc = Cipher.getInstance("AES/GCM/NoPadding")
            enc.init(Cipher.ENCRYPT_MODE, aesK, GCMParameterSpec(128, nonce))
            writePkt(dout, PKT_CERT, enc.doFinal(pubKeyBytes))
            Timber.d("$TAG sent encrypted RSA public key (${pubKeyBytes.size} raw bytes)")

            // ── 8. Read server's cert — AEADBadTagException = wrong key/code ───
            val srvCert = readPkt(din, PKT_CERT)
            val dec = Cipher.getInstance("AES/GCM/NoPadding")
            dec.init(Cipher.DECRYPT_MODE, aesK, GCMParameterSpec(128, nonce))
            dec.doFinal(srvCert)  // throws if authentication tag doesn't match

            Timber.i("$TAG ✓ pairing SUCCEEDED with $host:$port — device now trusts our key")
            true
        } finally {
            runCatching { socket.close() }
        }
    }

    // ── Packet I/O ────────────────────────────────────────────────────────────

    private fun readPkt(din: DataInputStream, expectedType: Int): ByteArray {
        val version = din.readUnsignedByte()
        val type    = din.readUnsignedByte()
        val size    = din.readInt()          // 4-byte big-endian
        check(version == PKT_VERSION) { "unexpected pairing packet version $version (expected $PKT_VERSION)" }
        check(type    == expectedType) { "unexpected packet type $type (expected $expectedType)" }
        check(size in 1..131072)      { "invalid payload size $size" }
        return ByteArray(size).also { din.readFully(it) }
    }

    private fun writePkt(dout: DataOutputStream, type: Int, payload: ByteArray) {
        dout.writeByte(PKT_VERSION)
        dout.writeByte(type)
        dout.writeInt(payload.size)      // 4-byte big-endian
        dout.write(payload)
        dout.flush()
    }

    // ── Crypto helpers ────────────────────────────────────────────────────────

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    /**
     * Build the SPAKE2 transcript hash.
     * Format (BoringSSL): SHA256 of [ u32be(|x|) || x ] for each element x in order.
     * Elements: label, aliceMsg, bobMsg, Kx (32 bytes), password, clientName, serverName
     */
    private fun sha256Transcript(vararg parts: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        for (p in parts) { md.update(u32be(p.size)); md.update(p) }
        return md.digest()
    }

    private fun u32be(v: Int): ByteArray = byteArrayOf(
        (v ushr 24 and 0xFF).toByte(),
        (v ushr 16 and 0xFF).toByte(),
        (v ushr  8 and 0xFF).toByte(),
        (v         and 0xFF).toByte()
    )

    /**
     * HKDF-SHA256 with zero salt, custom [info], output length [len] bytes.
     * Used to derive AES-256 key (32 bytes) + GCM nonce (12 bytes) from SPAKE2 key.
     */
    private fun hkdf(ikm: ByteArray, info: ByteArray, len: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        // Extract phase: salt = 0x00...00 (32 zero bytes, RFC 5869 §2.2)
        mac.init(SecretKeySpec(ByteArray(32), "HmacSHA256"))
        val prk = mac.doFinal(ikm)
        // Expand phase
        val out = ByteArray(len); var pos = 0; var T = ByteArray(0); var ctr = 1
        while (pos < len) {
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(T); mac.update(info); mac.update(ctr.toByte())
            T = mac.doFinal()
            val n = minOf(T.size, len - pos)
            T.copyInto(out, pos, 0, n); pos += n; ctr++
        }
        return out
    }

    /**
     * Read the ADB-format RSA public key from the .pub file written by dadb.
     * The file contains: "<base64-encoded-524-byte-ADB-key> unknown@unknown"
     */
    private fun readAdbPubKey(pubKeyFile: File): ByteArray {
        val text = pubKeyFile.readText().trim()
        val b64  = text.substringBefore(' ')
        return Base64.decode(b64, Base64.DEFAULT)
    }

    // ── P-256 affine point arithmetic (BigInteger) ────────────────────────────

    private data class Pt(val x: BigInteger, val y: BigInteger)

    /** Point addition — returns null to represent the point at infinity. */
    private fun ptAdd(A: Pt?, B: Pt?): Pt? {
        if (A == null) return B
        if (B == null) return A
        if (A.x == B.x) {
            return if (A.y != B.y || A.y.signum() == 0) null  // A = -B or A.y = 0
            else ptDouble(A)
        }
        val num = (B.y - A.y).mod(P256P)
        val den = (B.x - A.x).mod(P256P).modInverse(P256P)
        val s   = num * den % P256P
        val rx  = (s * s - A.x - B.x).mod(P256P)
        val ry  = (s * (A.x - rx) - A.y).mod(P256P)
        return Pt(rx, ry)
    }

    /** Point doubling. */
    private fun ptDouble(P: Pt): Pt {
        val num = (BigInteger.valueOf(3) * P.x.pow(2) + P256A).mod(P256P)
        val den = (BigInteger.valueOf(2) * P.y).mod(P256P).modInverse(P256P)
        val s   = num * den % P256P
        val rx  = (s.pow(2) - BigInteger.valueOf(2) * P.x).mod(P256P)
        val ry  = (s * (P.x - rx) - P.y).mod(P256P)
        return Pt(rx, ry)
    }

    /** Point negation: -P = (x, -y mod p). */
    private fun ptNeg(P: Pt) = Pt(P.x, (P256P - P.y).mod(P256P))

    /** Scalar multiplication: k * P using double-and-add. */
    private fun ptMul(k: BigInteger, P: Pt?): Pt? {
        if (P == null || k.signum() == 0) return null
        var R: Pt? = null; var Q: Pt = P
        var n = k.mod(P256N)
        while (n.signum() > 0) {
            if (n.testBit(0)) R = ptAdd(R, Q)
            Q = ptDouble(Q); n = n.shiftRight(1)
        }
        return R
    }

    /** Encode point as 65-byte uncompressed form (0x04 || x || y). */
    private fun ptEncode(P: Pt): ByteArray {
        val buf = ByteArray(65); buf[0] = 0x04
        pt32(P.x).copyInto(buf, 1); pt32(P.y).copyInto(buf, 33)
        return buf
    }

    /** Decode 65-byte uncompressed point. */
    private fun ptDecode(b: ByteArray): Pt {
        require(b.size == 65 && b[0] == 0x04.toByte()) {
            "bad point encoding: ${b.size} bytes, prefix=0x${b[0].toInt().and(0xFF).toString(16)}"
        }
        return Pt(BigInteger(1, b.copyOfRange(1, 33)), BigInteger(1, b.copyOfRange(33, 65)))
    }

    /** Serialize a BigInteger as exactly 32 big-endian bytes (for P-256 coordinates). */
    private fun pt32(v: BigInteger): ByteArray {
        val raw = v.toByteArray()
        return when {
            raw.size == 32 -> raw
            raw.size > 32  -> raw.copyOfRange(raw.size - 32, raw.size)  // strip leading 0x00
            else           -> ByteArray(32 - raw.size) + raw             // left-pad with zeros
        }
    }

    /** x-coordinate of a point as 32 big-endian bytes (used in SPAKE2 transcript). */
    private fun ptX32(P: Pt) = pt32(P.x)

    // ── Trust-all TLS ─────────────────────────────────────────────────────────

    private val TrustAll = object : X509TrustManager {
        override fun checkClientTrusted(c: Array<out X509Certificate>?, a: String?) {}
        override fun checkServerTrusted(c: Array<out X509Certificate>?, a: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
