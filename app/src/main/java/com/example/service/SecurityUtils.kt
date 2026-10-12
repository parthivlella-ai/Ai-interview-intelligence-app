package com.example.service

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {

    /**
     * Generates a cryptographically secure random hex salt.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hashes password with salt using SHA-256.
     * Plaintext passwords are never stored in database or preferences.
     */
    fun hashPassword(password: String, salt: String): String {
        val input = "$salt:$password"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies password against stored salt and hash.
     */
    fun verifyPassword(password: String, salt: String, storedHash: String): Boolean {
        val computedHash = hashPassword(password, salt)
        return computedHash == storedHash
    }
}
