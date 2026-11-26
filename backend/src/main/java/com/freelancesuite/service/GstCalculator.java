package com.freelancesuite.service;

import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.Client;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Indian GST split for a services invoice at the standard 18% rate.
 *
 * <ul>
 *   <li>An agency that is not GST-registered charges no GST.</li>
 *   <li>Supplier and client states come from their 2-digit GST state code,
 *       falling back to the first two characters of the GSTIN.</li>
 *   <li>Same state: CGST 9% + SGST 9%. Different states: IGST 18%.</li>
 *   <li>If the client's state is unknown, the place of supply is taken as the
 *       supplier's own state (intra-state), which is the default for services
 *       to an unregistered recipient without an address on record.</li>
 * </ul>
 */
public final class GstCalculator {

    private static final BigDecimal HALF_RATE = new BigDecimal("0.09");
    private static final BigDecimal FULL_RATE = new BigDecimal("0.18");

    private GstCalculator() {
    }

    public record TaxBreakdown(BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal totalAmount) {
    }

    public static TaxBreakdown calculate(BigDecimal subtotal, Agency agency, Client client) {
        BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        // Only an explicitly unregistered agency skips GST; missing data must not silently drop tax.
        if (agency != null && !agency.isGstRegistered()) {
            return new TaxBreakdown(zero, zero, zero, subtotal);
        }

        String agencyState = agency == null ? null : stateOf(agency.getStateCode(), agency.getGstin());
        String clientState = client == null ? null : stateOf(client.getStateCode(), client.getGstin());

        boolean interState = agencyState != null && clientState != null && !agencyState.equals(clientState);
        if (interState) {
            BigDecimal igst = subtotal.multiply(FULL_RATE).setScale(2, RoundingMode.HALF_UP);
            return new TaxBreakdown(zero, zero, igst, subtotal.add(igst));
        }

        BigDecimal half = subtotal.multiply(HALF_RATE).setScale(2, RoundingMode.HALF_UP);
        return new TaxBreakdown(half, half, zero, subtotal.add(half).add(half));
    }

    /** Returns the 2-digit state code, or null when neither source has one. */
    static String stateOf(String stateCode, String gstin) {
        if (stateCode != null && stateCode.trim().matches("\\d{2}")) {
            return stateCode.trim();
        }
        if (gstin != null && gstin.trim().length() >= 2 && gstin.trim().substring(0, 2).matches("\\d{2}")) {
            return gstin.trim().substring(0, 2);
        }
        return null;
    }
}
