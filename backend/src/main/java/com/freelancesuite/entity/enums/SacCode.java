package com.freelancesuite.entity.enums;

/**
 * Services Accounting Codes (SAC) under Chapter 99 of the CGST framework
 * for classifying software and IT services on GST invoices.
 *
 * @see <a href="https://www.xflowpay.com/blog/gst-on-software-services">SAC Code Reference</a>
 */
public enum SacCode {
    /**
     * SAC 998313 — Information technology consulting, technical support,
     * and annual maintenance contracts (AMC).
     */
    SAC_998313("998313", "IT Consulting & Support"),

    /**
     * SAC 998314 — Information technology design and development services
     * (custom code, full-stack application development, API design, native mobile apps).
     */
    SAC_998314("998314", "IT Design & Development"),

    /**
     * SAC 998315 — Hosting and IT infrastructure provisioning services
     * (cloud infrastructure setup, server management, SaaS hosting).
     */
    SAC_998315("998315", "Hosting & IT Infrastructure");

    private final String code;
    private final String description;

    SacCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() { return code; }
    public String getDescription() { return description; }
}
