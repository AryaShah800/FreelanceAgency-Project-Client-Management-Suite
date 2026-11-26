package com.freelancesuite.service;

import com.freelancesuite.entity.Agency;
import com.freelancesuite.entity.Client;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GstCalculatorTest {

    private static final BigDecimal SUBTOTAL = new BigDecimal("1000.00");

    private static Agency agency(String stateCode, String gstin, boolean registered) {
        Agency agency = Agency.builder().name("Agency").stateCode(stateCode).gstin(gstin).build();
        agency.setGstRegistered(registered);
        return agency;
    }

    private static Client client(String stateCode, String gstin) {
        return Client.builder().companyName("Client").stateCode(stateCode).gstin(gstin).build();
    }

    private static void assertTax(GstCalculator.TaxBreakdown tax, String cgst, String sgst, String igst, String total) {
        assertEquals(0, new BigDecimal(cgst).compareTo(tax.cgst()), "cgst");
        assertEquals(0, new BigDecimal(sgst).compareTo(tax.sgst()), "sgst");
        assertEquals(0, new BigDecimal(igst).compareTo(tax.igst()), "igst");
        assertEquals(0, new BigDecimal(total).compareTo(tax.totalAmount()), "total");
    }

    @Test
    void sameStateChargesCgstAndSgst() {
        assertTax(GstCalculator.calculate(SUBTOTAL, agency("27", null, true), client("27", null)),
                "90", "90", "0", "1180");
    }

    @Test
    void differentStateChargesIgst() {
        assertTax(GstCalculator.calculate(SUBTOTAL, agency("27", null, true), client("24", null)),
                "0", "0", "180", "1180");
    }

    @Test
    void interStateClientWithoutGstinUsesItsStateCode() {
        // Previously both sides defaulted to "27" when the client had no GSTIN.
        assertTax(GstCalculator.calculate(SUBTOTAL, agency(null, "27AAAAA0000A1Z5", true), client("29", null)),
                "0", "0", "180", "1180");
    }

    @Test
    void unregisteredAgencyChargesNoGst() {
        assertTax(GstCalculator.calculate(SUBTOTAL, agency("27", null, false), client("24", null)),
                "0", "0", "0", "1000");
    }

    @Test
    void unknownClientStateIsTreatedAsIntraState() {
        assertTax(GstCalculator.calculate(SUBTOTAL, agency("27", null, true), client(null, null)),
                "90", "90", "0", "1180");
    }

    @Test
    void stateCodeTakesPrecedenceOverGstinPrefix() {
        assertEquals("24", GstCalculator.stateOf("24", "27AAAAA0000A1Z5"));
        assertEquals("27", GstCalculator.stateOf(" ", "27AAAAA0000A1Z5"));
        assertNull(GstCalculator.stateOf(null, "XX"));
    }

    @Test
    void roundsEachComponentToPaise() {
        assertTax(GstCalculator.calculate(new BigDecimal("333.33"), agency("27", null, true), client("27", null)),
                "30.00", "30.00", "0", "393.33");
    }
}
