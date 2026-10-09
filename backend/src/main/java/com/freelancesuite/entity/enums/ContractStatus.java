package com.freelancesuite.entity.enums;

/** Status labels for the contract lifecycle; transition rules need service-layer enforcement. */
public enum ContractStatus {
    /** Contract version is being drafted. */
    DRAFT,

    /** Internal agency review is in progress. */
    PENDING_REVIEW,

    /** Prepared for signing. */
    PENDING_SIGNATURE,

    /** Marked active by a future contract workflow. */
    ACTIVE,

    /** Superseded by a newer version via a formal change order. */
    AMENDED,

    /** Contract terminated (breach or convenience). */
    TERMINATED,

    /** A signer rejected the version; requires a new draft. */
    REJECTED
}
