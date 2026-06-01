package com.accu.connection

import timber.log.Timber
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.net.ssl.SSLSocket

/**
 * ADB-over-TLS client matching the exact STLS upgrade protocol used by Android 11+
 * wireless ADB session ports.
 *
 * Protocol (mirrored from Shizuku AdbClient.kt / AOSP adb.h):
 *   1. Plain TCP connect
 *   2. Client → Server : A_CNXN  (we initiate)
 *   3. Server → Client : A_STLS  (server requests TLS upgrade)
 *   4. Client → Server : A_STLS  (we agree)
 *   5. TLS 1.3 handshake over the same TCP socket (mTLS — adbd verifies client cert
 *      against the key registered during SPAKE2 pairing)
 *   6. Server → Client : A_CNXN  (device banner, connection established)
 *
 * WHY our previous implementation failed:
 *   We called ssl.startHandshake() immediately after TCP connect, sending TLS
 *   ClientHello to a server expecting an ADB A_CNXN packet.  The server couldn't
 *   parse it, closed the connection → SSLHandshakeException("connection closed")
 *   every time, regardless of key correctness.
 *
 * Reference: ReferenceRepo/Shizuku/manager/adb/AdbClient.kt
 *            AOSP packages/modules/adb/adb.h
 */
class AdbWifiConnectClient private constructor(private val adbKey: AdbKey) : AutoCloseable {

    private var socket: Socket? = null
    private var sslSocket: SSLSocket? = null
    private lateinit var din: DataInputStream
    private lateinit var dout: DataOutputStream

    companion object {
        private const val TAG = "AdbWifiConnect"

        // ── ADB protocol commands (little-endian magic bytes) ─────────────────
        private const val A_CNXN = 0x4e584e43
        private const val A_AUTH = 0x48545541
        private const val A_OPEN = 0x4e45504f
        private const val A_OKAY = 0x59414b4f
        private const val A_CLSE = 0x45534c43
        private const val A_WRTE = 0x45545257
        private const val A_STLS = 0x534C5453

        private const val ADB_AUTH_SIGNATURE    = 2
        private const val ADB_AUTH_RSAPUBLICKEY = 3

        private const val A_VERSION      = 0x01000000
        private const val A_STLS_VERSION = 0x01000000
        private const val A_MAXDATA      = 262144   // 256 KB max payload

        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val SO_TIMEOUT_MS      = 20_000

        private const val CONN_STRING =
            "host::features=shell_v2,cmd,stat_v2,ls_v2,fixed_push_mkdir,apex"

        /**
         * Connect to an Android 11+ wireless ADB session port.
         *
         * The [adbKey] must be the SAME key used during SPAKE2 pairing —
         * adbd validates the mTLS client cert against its registered key list.
         *
         * @throws Exception if the ADB handshake or TLS upgrade fails.
         */
        fun connect(host: String, port: Int, adbKey: AdbKey): AdbWifiConnectClient {
            val client = AdbWifiConnectClient(adbKey)
            client.open(host, port)
            return client
        }
    }

    private fun open(host: String, port: Int) {
        // ── 1. Plain TCP connect ───────────────────────────────────────────────
        val sock = Socket()
        sock.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
        sock.tcpNoDelay = true
        sock.soTimeout = SO_TIMEOUT_MS
        socket = sock
        din  = DataInputStream(sock.getInputStream())
        dout = DataOutputStream(sock.getOutputStream())

        // ── 2. Client sends A_CNXN ─────────────────────────────────────────────
        writeMsg(A_CNXN, A_VERSION, A_MAXDATA, CONN_STRING.toByteArray(Charsets.UTF_8))
        Timber.d("$TAG → A_CNXN sent to $host:$port")

        // ── 3. Read server response ────────────────────────────────────────────
        val msg = readMsg()
        Timber.d("$TAG ← cmd=0x${msg.cmd.toString(16)} arg0=0x${msg.arg0.toString(16)}")

        when (msg.cmd) {
            A_STLS -> {
                // Android 11+ wireless ADB: server requests TLS upgrade
                // ── 4. Client agrees to TLS upgrade ───────────────────────────
                writeMsg(A_STLS, A_STLS_VERSION, 0, ByteArray(0))
                Timber.d("$TAG → A_STLS sent — upgrading to TLS")

                // ── 5. TLS 1.3 handshake over the existing TCP socket ──────────
                // createSocket(existing, ...) wraps the TCP stream — NOT a new conn.
                val ssl = adbKey.sslContext.socketFactory
                    .createSocket(sock, host, port, /*autoClose=*/ true) as SSLSocket
                ssl.soTimeout = SO_TIMEOUT_MS
                ssl.useClientMode = true
                ssl.enabledProtocols = arrayOf("TLSv1.3")

                try {
                    ssl.startHandshake()
                    Timber.i("$TAG TLS handshake OK — suite=${ssl.session.cipherSuite}")
                } catch (e: Exception) {
                    throw IllegalStateException(
                        "TLS handshake failed after STLS upgrade ($host:$port). " +
                        "Most likely cause: key registered during pairing does not match " +
                        "TLS client cert. Re-pair the device (Developer Options → " +
                        "Wireless Debugging → pair with code).", e)
                }
                sslSocket = ssl
                din  = DataInputStream(ssl.inputStream)
                dout = DataOutputStream(ssl.outputStream)

                // ── 6. Read A_CNXN from server (no further AUTH needed after mTLS) ──
                val cnxn = readMsg()
                if (cnxn.cmd != A_CNXN) {
                    throw IllegalStateException(
                        "Expected A_CNXN after TLS handshake, got 0x${cnxn.cmd.toString(16)}")
                }
                Timber.i("$TAG Connected via STLS+TLS ✓ — " +
                    "device=${String(cnxn.data, Charsets.UTF_8).take(120)}")
            }

            A_AUTH -> {
                // Fallback: older pre-wireless AUTH challenge path
                // (rare on Android 11+ wireless ADB, but handled for robustness)
                Timber.d("$TAG ← A_AUTH token — signing")
                writeMsg(A_AUTH, ADB_AUTH_SIGNATURE, 0, adbKey.sign(msg.data))
                val next = readMsg()
                if (next.cmd != A_CNXN) {
                    // Token not accepted — send full RSA public key for manual approval
                    writeMsg(A_AUTH, ADB_AUTH_RSAPUBLICKEY, 0, adbKey.adbPublicKey)
                    val cnxn = readMsg()
                    if (cnxn.cmd != A_CNXN) {
                        throw IllegalStateException("ADB AUTH rejected — device did not accept our public key")
                    }
                }
                Timber.i("$TAG Connected via AUTH path ✓")
            }

            else -> throw IllegalStateException(
                "Unexpected first ADB message: 0x${msg.cmd.toString(16)} — " +
                "expected A_STLS (0x534C5453) or A_AUTH (0x48545541)")
        }
    }

    /**
     * Execute a shell command on the remote device.
     * Returns combined stdout+stderr as a trimmed string.
     */
    fun shell(command: String): String {
        val localId = (System.nanoTime() and 0x7FFF_FFFFL).toInt().coerceAtLeast(1)
        writeMsg(A_OPEN, localId, 0, "shell:$command".toByteArray(Charsets.UTF_8))

        var remoteId = 0
        val output   = StringBuilder()
        var open     = true

        while (open) {
            val msg = readMsg()
            when (msg.cmd) {
                A_OKAY -> if (msg.arg1 == localId) remoteId = msg.arg0
                A_WRTE -> if (msg.arg1 == localId) {
                    output.append(String(msg.data, Charsets.UTF_8))
                    writeMsg(A_OKAY, localId, msg.arg0, ByteArray(0))
                }
                A_CLSE -> if (msg.arg1 == localId) {
                    if (remoteId > 0) writeMsg(A_CLSE, localId, remoteId, ByteArray(0))
                    open = false
                }
                else -> Timber.d("$TAG shell: ignored cmd=0x${msg.cmd.toString(16)}")
            }
        }
        return output.toString().trimEnd('\n')
    }

    override fun close() {
        try { sslSocket?.close() } catch (_: Exception) {}
        try { socket?.close()   } catch (_: Exception) {}
        sslSocket = null
        socket    = null
    }

    private data class Msg(val cmd: Int, val arg0: Int, val arg1: Int, val data: ByteArray)

    private fun writeMsg(cmd: Int, arg0: Int, arg1: Int, data: ByteArray) {
        val check = data.fold(0) { acc, b -> acc + (b.toInt() and 0xFF) }
        val hdr = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(cmd); putInt(arg0); putInt(arg1)
            putInt(data.size); putInt(check); putInt(cmd.inv())
        }
        synchronized(dout) {
            dout.write(hdr.array())
            if (data.isNotEmpty()) dout.write(data)
            dout.flush()
        }
    }

    private fun readMsg(): Msg {
        val hdr = ByteArray(24).also { din.readFully(it) }
        val buf = ByteBuffer.wrap(hdr).order(ByteOrder.LITTLE_ENDIAN)
        val cmd = buf.int; val arg0 = buf.int; val arg1 = buf.int
        val len = buf.int; buf.int; buf.int  // skip checksum and magic
        val data = if (len > 0) ByteArray(len).also { din.readFully(it) } else ByteArray(0)
        return Msg(cmd, arg0, arg1, data)
    }
}
