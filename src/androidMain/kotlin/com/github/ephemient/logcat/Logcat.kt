package com.github.ephemient.logcat

import android.os.Build
import android.system.ErrnoException
import android.util.CloseGuard
import java.lang.ref.Reference
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.datetime.Instant

class Logcat(nonblocking: Boolean = false, tail: Int = 0) : Object(), AutoCloseable {
    private var ptr: Long = nativeAlloc(if (nonblocking) ANDROID_LOG_NONBLOCK else 0, tail)
    init {
        check(ptr != 0L) { "android_logger_list_alloc" }
    }
    private val closeGuard = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        CloseGuard().apply { open("close") }
    } else {
        null
    }
    private val buffer = ByteBuffer.allocateDirect(LOGGER_ENTRY_MAX_LEN).order(ByteOrder.nativeOrder())

    fun open(logId: Int = LOG_ID_MAIN) {
        check(nativeOpen(ptr, logId) != 0L) { "android_logger_open" }
    }

    fun open(logName: String) {
        open(logId(logName))
    }

    fun readMessage(): Message? {
        buffer.clear()
        val ret = nativeReadMessage(ptr, buffer)
        if (ret < 0) throw ErrnoException("android_logger_list_read", -ret)
        if (ret < 24) return null
        buffer.limit(ret)
        val len = buffer.getShort(0).toUShort().toInt()
        if (len < 3) return null
        val hdrSize = buffer.getShort(2).toUShort().toInt()
        val lid: Int
        val uid: Int
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.M -> {
                if (hdrSize != 24) return null
                lid = 0
                uid = buffer.getInt(20)
            }
            Build.VERSION.SDK_INT < Build.VERSION_CODES.N -> {
                if (hdrSize != 24) return null
                lid = buffer.getInt(20)
                uid = 0
            }
            else -> {
                if (hdrSize < 28) return null
                lid = buffer.getInt(20)
                uid = buffer.getInt(24)
            }
        }
        buffer.position(hdrSize + 1)
        val bytes = ByteArray(len - 1)
        buffer.get(bytes)
        val message = bytes.decodeToString().removeSuffix("\u0000")
        val tagLength = message.indexOf('\u0000')
        return Message(
            pid = buffer.getInt(4),
            tid = buffer.getInt(8),
            time = Instant.fromEpochSeconds(buffer.getInt(12).toLong(), buffer.getInt(16)),
            lid = lid,
            uid = uid,
            priority = buffer.get(hdrSize).toUByte().toInt(),
            tag = if (tagLength < 0) "" else message.substring(0, tagLength),
            message = message.substring(tagLength + 1),
        )
    }

    override fun close() {
        nativeClose(ptr)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            closeGuard?.close()
            Reference.reachabilityFence(this)
        }
    }

    override fun finalize() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) closeGuard?.warnIfOpen()
            close()
        } finally {
            super.finalize();
        }
    }

    private external fun nativeAlloc(mode: Int, tail: Int): Long
    private external fun nativeOpen(ptr: Long, logId: Int): Long
    private external fun nativeReadMessage(ptr: Long, buf: Buffer): Int
    private external fun nativeClose(ptr: Long)

    override fun toString(): String = "Logcat@${ptr.toString(16)}"

    data class Message(
        val pid: Int,
        val tid: Int,
        val time: Instant,
        val lid: Int,
        val uid: Int,
        val priority: Int,
        val tag: String,
        val message: String,
    )

    companion object {
        init {
            System.loadLibrary("nativelogcat")
        }

        const val LOG_ID_MAIN = 0
        const val LOG_ID_RADIO = 1
        const val LOG_ID_EVENTS = 2
        const val LOG_ID_SYSTEM = 3
        const val LOG_ID_CRASH = 4
        const val LOG_ID_STATS = 5
        const val LOG_ID_SECURITY = 6
        const val LOG_ID_KERNEL = 7

        private const val LOGGER_ENTRY_MAX_LEN = 5 * 1024
        private const val ANDROID_LOG_NONBLOCK = 0x00000800

        external fun logId(logName: String): Int
        external fun logName(logId: Int): String?
    }
}
