package com.freelancesuite.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Shared HMAC-SHA256 verification used for both the client-triggered
 * Razorpay "verify" call and the server-to-server Razorpay webhook.
 * Kept as a small pure/static utility so it can be unit tested without
 * a Spring context.
 */
public final class HmacSignatureUtil {

    private HmacSignatureUtil() {
    }

    /**
     * Computes HMAC-SHA256(payload, secret) and compares it, in constant time,
     * against the provided hex-encoded signature.
     *
     * @param payload         the exact raw string/body the signature was generated over
     * @param secret          the shared secret (Razorpay key secret or webhook secret)
     * @param signatureHex    the hex-encoded signature to verify against
     * @return true if the signature is valid, false otherwise (never throws on bad input)
     */
    public static boolean verify(String payload, String secret, String signatureHex) {
        if (payload == null || secret == null || signatureHex == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] provided = hexToBytes(signatureHex);
            return provided.length > 0 && MessageDigest.isEqual(expected, provided);
        } catch (Exception ex) {
            return false;
        }
    }

    public static byte[] hexToBytes(String value) {
        if (value == null || value.length() % 2 != 0 || !value.matches("[0-9a-fA-F]+")) {
            return new byte[0];
        }
        byte[] bytes = new byte[value.length() / 2];
        for (int index = 0; index < value.length(); index += 2) {
            bytes[index / 2] = (byte) Integer.parseInt(value.substring(index, index + 2), 16);
        }
        return bytes;
    }

    /** Convenience helper for computing a hex-encoded HMAC-SHA256, used when we need to store a hash (not just verify one). */
    public static String hmacHex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Could not compute HMAC signature", ex);
        }
    }
}
