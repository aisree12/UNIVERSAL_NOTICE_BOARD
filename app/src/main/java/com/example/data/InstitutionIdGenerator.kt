package com.example.data

import java.security.MessageDigest
import java.security.SecureRandom

object InstitutionIdGenerator {
    private val random = SecureRandom()
    private const val ALPHANUMERIC = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"

    /**
     * Generates a readable, permanent Institution ID following Section 4:
     * NB-[NORMALIZED-NAME]-[SHORT-UNIQUE-SUFFIX]
     */
    fun generateCandidateId(institutionName: String): String {
        val slug = normalizeSlug(institutionName)
        val suffix = buildString {
            repeat(5) {
                append(ALPHANUMERIC[random.nextInt(ALPHANUMERIC.length)])
            }
        }
        return "NB-$slug-$suffix"
    }

    fun normalizeSlug(institutionName: String): String {
        val words = institutionName
            .uppercase()
            .replace(Regex("[^A-Z0-9\\s]"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val slug = when {
            words.isEmpty() -> "CAMPUS"
            words.size == 1 -> words[0].take(8)
            words.size <= 3 -> words.joinToString("") { it.take(3) }.take(8)
            else -> words.map { it.first() }.joinToString("").take(8)
        }
        return slug.ifBlank { "CAMPUS" }
    }

    /**
     * Computes salted SHA-256 hash scoped to institutionId so plaintext passcodes
     * are never stored in Firestore or source code.
     */
    fun hashPasscode(institutionId: String, passcode: String): String {
        val input = "${institutionId.trim().uppercase()}:${passcode.trim()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun normalizeNoticeTitle(title: String): String {
        return title.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
