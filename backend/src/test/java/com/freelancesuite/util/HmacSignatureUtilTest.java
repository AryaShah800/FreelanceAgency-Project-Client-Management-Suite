package com.freelancesuite.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HmacSignatureUtilTest {

    private static final String SECRET = "test_secret_key";

    @Test
    void verifyAcceptsACorrectlyComputedSignature() {
        String payload = "order_123|pay_456";
        String signature = HmacSignatureUtil.hmacHex(payload, SECRET);

        assertTrue(HmacSignatureUtil.verify(payload, SECRET, signature));
    }

    @Test
    void verifyRejectsATamperedPayload() {
        String signature = HmacSignatureUtil.hmacHex("order_123|pay_456", SECRET);

        assertFalse(HmacSignatureUtil.verify("order_123|pay_999", SECRET, signature));
    }

    @Test
    void verifyRejectsTheWrongSecret() {
        String payload = "order_123|pay_456";
        String signature = HmacSignatureUtil.hmacHex(payload, SECRET);

        assertFalse(HmacSignatureUtil.verify(payload, "a_different_secret", signature));
    }

    @Test
    void verifyRejectsAMalformedSignature() {
        assertFalse(HmacSignatureUtil.verify("order_123|pay_456", SECRET, "not-hex!!"));
    }

    @Test
    void verifyRejectsNullInputsWithoutThrowing() {
        assertFalse(HmacSignatureUtil.verify(null, SECRET, "aa"));
        assertFalse(HmacSignatureUtil.verify("payload", null, "aa"));
        assertFalse(HmacSignatureUtil.verify("payload", SECRET, null));
    }

    @Test
    void hmacHexIsDeterministic() {
        String payload = "order_abc|pay_xyz";
        assertTrue(HmacSignatureUtil.hmacHex(payload, SECRET).equals(HmacSignatureUtil.hmacHex(payload, SECRET)));
    }
}
