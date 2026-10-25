package com.example.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PinHashResult(
  val saltHex: String,
  val hashHex: String
)

/**
 * High-assurance cryptographic manager for PIN derivation and validation.
 * Uses PBKDF2WithHmacSHA256 with random salting and constant-time comparison
 * to eliminate timing-channel vulnerabilities.
 */
object CryptoManager {
  private const val ALGORITHM = "PBKDF2WithHmacSHA256"
  private const val ITERATIONS = 10_000
  private const val KEY_LENGTH_BITS = 256
  private const val SALT_LENGTH_BYTES = 16

  private val secureRandom = SecureRandom()

  /**
   * Generates a cryptographically strong random salt.
   */
  fun generateSalt(): ByteArray {
    val salt = ByteArray(SALT_LENGTH_BYTES)
    secureRandom.nextBytes(salt)
    return salt
  }

  /**
   * Hashes a PIN using PBKDF2 with a newly generated random salt.
   */
  fun hashPin(pin: String): PinHashResult {
    val salt = generateSalt()
    val hash = deriveKey(pin, salt)
    return PinHashResult(
      saltHex = bytesToHex(salt),
      hashHex = bytesToHex(hash)
    )
  }

  /**
   * Verifies an input PIN against stored salt and hash using constant-time comparison.
   */
  fun verifyPin(pin: String, storedSaltHex: String, storedHashHex: String): Boolean {
    if (storedSaltHex.isBlank() || storedHashHex.isBlank()) return false
    return try {
      val salt = hexToBytes(storedSaltHex)
      val expectedHash = hexToBytes(storedHashHex)
      val computedHash = deriveKey(pin, salt)
      MessageDigest.isEqual(expectedHash, computedHash)
    } catch (_: Exception) {
      false
    }
  }

  private fun deriveKey(pin: String, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
    val factory = SecretKeyFactory.getInstance(ALGORITHM)
    return factory.generateSecret(spec).encoded
  }

  fun bytesToHex(bytes: ByteArray): String {
    val sb = StringBuilder(bytes.size * 2)
    for (b in bytes) {
      sb.append(String.format("%02x", b.toInt() and 0xFF))
    }
    return sb.toString()
  }

  fun hexToBytes(hex: String): ByteArray {
    val len = hex.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
      data[i / 2] = (
        (Character.digit(hex[i], 16) shl 4) +
          Character.digit(hex[i + 1], 16)
        ).toByte()
      i += 2
    }
    return data
  }
}
