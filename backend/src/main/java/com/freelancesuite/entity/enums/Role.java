package com.freelancesuite.entity.enums;

public enum Role {
    OWNER,
    ADMIN,
    /** Existing agency members are retained as the staff role for compatibility. */
    MEMBER,
    CLIENT
}
