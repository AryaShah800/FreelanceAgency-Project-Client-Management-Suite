package com.freelancesuite.entity.enums;

/** Signature method labels for future integrations; these values do not perform signing or identity verification. */
public enum SignatureType {
    /** A clickwrap-style assent label; no OTP or identity check is implemented here. */
    CLICKWRAP_E_SIGN,

    /** Reserved for an integrated, licensed Aadhaar eSign provider. */
    AADHAAR_ESIGN,

    /** Reserved for an integrated digital-signature certificate provider. */
    DSC
}
