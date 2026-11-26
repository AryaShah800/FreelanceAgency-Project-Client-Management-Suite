package com.freelancesuite.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDto(Long id, Long invoiceId, BigDecimal amount, String razorpayPaymentId,
                         String razorpayOrderId, LocalDateTime paidAt) {
}
