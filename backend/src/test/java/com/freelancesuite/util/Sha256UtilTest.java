package com.freelancesuite.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Sha256Util} — SHA-256 hex digest utility used for
 * canonical JSON hashing, PDF binary integrity, and audit chain events.
 */
class Sha256UtilTest {

    @Test
    void hexDigest_producesCorrect64CharHexString() {
        // SHA-256("hello") = 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
        String result = Sha256Util.hexDigest("hello");
        assertEquals(64, result.length());
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", result);
    }

    @Test
    void hexDigest_emptyStringProducesKnownHash() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        String result = Sha256Util.hexDigest("");
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", result);
    }

    @Test
    void hexDigest_identicalInputsProduceIdenticalHashes() {
        String a = Sha256Util.hexDigest("contract|data|v1");
        String b = Sha256Util.hexDigest("contract|data|v1");
        assertEquals(a, b);
    }

    @Test
    void hexDigest_differentInputsProduceDifferentHashes() {
        String a = Sha256Util.hexDigest("contract|data|v1");
        String b = Sha256Util.hexDigest("contract|data|v2");
        assertNotEquals(a, b);
    }

    @Test
    void hexDigest_nullStringThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> Sha256Util.hexDigest((String) null));
    }

    @Test
    void hexDigest_nullBytesThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> Sha256Util.hexDigest((byte[]) null));
    }

    @Test
    void hexDigest_bytesMatchesStringForUtf8() {
        String input = "canonical-json-payload";
        String fromString = Sha256Util.hexDigest(input);
        String fromBytes = Sha256Util.hexDigest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(fromString, fromBytes);
    }

    @Test
    void verify_matchingDigestsReturnTrue() {
        String hash = Sha256Util.hexDigest("test-payload");
        assertTrue(Sha256Util.verify(hash, hash));
    }

    @Test
    void verify_mismatchedDigestsReturnFalse() {
        String a = Sha256Util.hexDigest("payload-a");
        String b = Sha256Util.hexDigest("payload-b");
        assertFalse(Sha256Util.verify(a, b));
    }

    @Test
    void verify_nullInputsReturnFalse() {
        assertFalse(Sha256Util.verify(null, "abc"));
        assertFalse(Sha256Util.verify("abc", null));
        assertFalse(Sha256Util.verify(null, null));
    }

    /**
     * Simulates an audit chain hash computation to verify the concatenation
     * formula produces stable results.
     */
    @Test
    void auditChainHash_formula_isStable() {
        String prevHash = Sha256Util.hexDigest("genesis-event");
        String eventData = "1|2026-10-01T10:00:00|CONTRACT_CREATED|alex@agency.com|payload-digest|" + prevHash;
        String eventHash = Sha256Util.hexDigest(eventData);

        // Same inputs should always produce the same chain hash
        String eventHash2 = Sha256Util.hexDigest(eventData);
        assertEquals(eventHash, eventHash2);
        assertEquals(64, eventHash.length());
    }
}
