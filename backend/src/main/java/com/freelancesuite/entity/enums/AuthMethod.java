package com.freelancesuite.entity.enums;

/** Authentication method labels reserved for a future contract-signing workflow. */
public enum AuthMethod {
    /** Email magic link and one-time password; no flow is currently implemented. */
    MAGIC_LINK_OTP,

    /** Standard application sign-in. */
    PASSWORD,

    /** Digital-signature certificate token; no provider is integrated. */
    DSC_TOKEN
}
