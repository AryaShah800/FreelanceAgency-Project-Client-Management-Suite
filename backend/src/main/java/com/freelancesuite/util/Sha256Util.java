package com.freelancesuite.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Pure SHA-256 hex digest utility for document integrity and audit hash chains.
 *
 * <p>Unlike {@link HmacSignatureUtil} (which computes keyed HMAC-SHA256 for
 * Razorpay signature verification), this utility computes standard, unkeyed
 * SHA-256 digests for:</p>
 * <ul>
 *     <li>Canonical JSON semantic integrity hashes</li>
 *     <li>PDF binary integrity hashes</li>
 *     <li>Audit event chain hashes</li>
 * </ul>
 */
public final class Sha256Util {

    private Sha256Util() {}

    /**
     * Computes the SHA-256 hex digest of a UTF-8 string.
     *
     * @param input the string to hash
     * @return lowercase 64-character hex digest
     */
    public static String hexDigest(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Cannot compute SHA-256 of null input");
        }
        return hexDigest(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Computes the SHA-256 hex digest of raw bytes.
     *
     * @param input the bytes to hash
     * @return lowercase 64-character hex digest
     */
    public static String hexDigest(byte[] input) {
        if (input == null) {
            throw new IllegalArgumentException("Cannot compute SHA-256 of null input");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is mandated by the Java specification; this should never happen.
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

    /**
     * Verifies that a hex digest matches the expected value in constant time.
     *
     * @param actual   the computed hex digest
     * @param expected the expected hex digest
     * @return true if they match
     */
    public static boolean verify(String actual, String expected) {
        if (actual == null || expected == null) return false;
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
