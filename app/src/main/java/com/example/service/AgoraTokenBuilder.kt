package com.example.service

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.TreeMap
import java.util.zip.CRC32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Generates Agora RTC Dynamic Key / Token (Version 006) for authenticated voice/video calling.
 * Compliant with AgoraIO official specification.
 */
object AgoraTokenBuilder {

    private const val VERSION = "006"

    private class ByteBuf(capacity: Int = 1024) {
        private var buffer: ByteBuffer = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN)

        fun asBytes(): ByteArray {
            val out = ByteArray(buffer.position())
            buffer.rewind()
            buffer.get(out, 0, out.size)
            return out
        }

        private fun ensureCapacity(additional: Int) {
            val required = buffer.position() + additional
            if (required <= buffer.capacity()) return
            var cap = buffer.capacity()
            while (cap < required) {
                cap *= 2
            }
            val expanded = ByteBuffer.allocate(cap).order(ByteOrder.LITTLE_ENDIAN)
            buffer.flip()
            expanded.put(buffer)
            buffer = expanded
        }

        fun putShort(v: Short): ByteBuf {
            ensureCapacity(2)
            buffer.putShort(v)
            return this
        }

        fun putInt(v: Int): ByteBuf {
            ensureCapacity(4)
            buffer.putInt(v)
            return this
        }

        fun putBytes(v: ByteArray): ByteBuf {
            putShort(v.size.toShort())
            ensureCapacity(v.size)
            buffer.put(v)
            return this
        }

        fun putIntMap(map: TreeMap<Short, Int>): ByteBuf {
            putShort(map.size.toShort())
            for ((key, value) in map) {
                putShort(key)
                putInt(value)
            }
            return this
        }
    }

    private fun crc32(bytes: ByteArray): Int {
        val crc = CRC32()
        crc.update(bytes)
        return crc.value.toInt()
    }

    private fun hmacSign(key: String, message: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)
        return mac.doFinal(message)
    }

    /**
     * Builds an Agora RTC Token (006) with broadcaster privileges for voice/video communication.
     */
    fun buildToken(
        appId: String,
        appCertificate: String,
        channelName: String,
        uid: Int = 0,
        expireSeconds: Int = 86400 // 24 hours
    ): String {
        if (appId.length != 32 || appCertificate.length != 32) {
            return ""
        }

        val uidStr = if (uid == 0) "" else uid.toString()
        val currentTs = (System.currentTimeMillis() / 1000).toInt()
        val privilegeTs = currentTs + expireSeconds
        val salt = SecureRandom().nextInt()

        // 1. Pack PrivilegeMessage
        val privileges = TreeMap<Short, Int>().apply {
            put(1.toShort(), privilegeTs) // kJoinChannel
            put(2.toShort(), privilegeTs) // kPublishAudioStream
            put(3.toShort(), privilegeTs) // kPublishVideoStream
            put(4.toShort(), privilegeTs) // kPublishDataStream
        }

        val msgBuf = ByteBuf()
            .putInt(salt)
            .putInt(privilegeTs)
            .putIntMap(privileges)
        val messageRawContent = msgBuf.asBytes()

        // 2. Generate Signature
        val baos = ByteArrayOutputStream()
        baos.write(appId.toByteArray(StandardCharsets.UTF_8))
        baos.write(channelName.toByteArray(StandardCharsets.UTF_8))
        baos.write(uidStr.toByteArray(StandardCharsets.UTF_8))
        baos.write(messageRawContent)
        val signature = hmacSign(appCertificate, baos.toByteArray())

        // 3. Checksums
        val crcChannel = crc32(channelName.toByteArray(StandardCharsets.UTF_8))
        val crcUid = if (uidStr.isEmpty()) 0 else crc32(uidStr.toByteArray(StandardCharsets.UTF_8))

        // 4. Pack final content
        val packBuf = ByteBuf()
            .putBytes(signature)
            .putInt(crcChannel)
            .putInt(crcUid)
            .putBytes(messageRawContent)

        val packedBytes = packBuf.asBytes()
        val encodedBase64 = try {
            Base64.encodeToString(packedBytes, Base64.NO_WRAP)
        } catch (_: Throwable) {
            java.util.Base64.getEncoder().encodeToString(packedBytes)
        }

        return VERSION + appId + encodedBase64
    }
}
