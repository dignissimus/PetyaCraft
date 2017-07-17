package me.ezeh.ransome.petyacraft

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec


object EncryptionTools {
    fun encryptByteArray(key: ByteArray, content: ByteArray): ByteArray {
        val secret = SecretKeySpec(key, "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, secret)
        return cipher.doFinal(content)
    }

    fun generateKey(): ByteArray {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256)
        // SecretKeySpec(generator.generateKey().encoded, "AES") // I don't know why that's there
        return generator.generateKey().encoded
    }
}