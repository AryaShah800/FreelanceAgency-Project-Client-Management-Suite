package com.freelancesuite.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static Set<String> invalidFields(Object dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void invoiceNeedsAtLeastOneValidLineItem() {
        InvoiceDto empty = new InvoiceDto();
        empty.setProjectId(1L);
        empty.setLineItems(List.of());
        assertTrue(invalidFields(empty).contains("lineItems"));

        InvoiceLineItemDto badItem = new InvoiceLineItemDto();
        badItem.setDescription("");
        badItem.setQuantity(0);
        badItem.setUnitPrice(new BigDecimal("-1"));
        InvoiceDto withBadItem = new InvoiceDto();
        withBadItem.setProjectId(1L);
        withBadItem.setLineItems(List.of(badItem));
        // @Valid cascades into the items.
        assertTrue(invalidFields(withBadItem).stream().anyMatch(f -> f.startsWith("lineItems[0].")));
    }

    @Test
    void expenseRejectsMissingOrNonPositiveAmounts() {
        ExpenseDto expense = new ExpenseDto();
        expense.setDescription("Hosting");
        expense.setCategory("Infra");
        expense.setAmount(new BigDecimal("-5"));
        assertEquals(Set.of("amount"), invalidFields(expense));
        expense.setAmount(null);
        assertEquals(Set.of("amount"), invalidFields(expense));
        expense.setAmount(new BigDecimal("12.50"));
        assertTrue(invalidFields(expense).isEmpty());
    }

    @Test
    void clientGstinIsOptionalButMustBeWellFormed() {
        ClientDto client = new ClientDto();
        client.setCompanyName("Acme");
        client.setEmail("a@acme.test");
        client.setGstin("");
        assertTrue(invalidFields(client).isEmpty());
        client.setGstin("27AAACA0000A1Z5");
        assertTrue(invalidFields(client).isEmpty());
        client.setGstin("NOT-A-GSTIN");
        assertEquals(Set.of("gstin"), invalidFields(client));
    }

    @Test
    void projectBudgetAndTaskHoursCannotBeNegative() {
        ProjectDto project = new ProjectDto();
        project.setClientId(1L);
        project.setTitle("Site");
        project.setBudget(new BigDecimal("-1"));
        assertEquals(Set.of("budget"), invalidFields(project));

        TaskDto task = new TaskDto();
        task.setProjectId(1L);
        task.setTitle("Build");
        task.setEstimatedHours(-2.0);
        assertEquals(Set.of("estimatedHours"), invalidFields(task));
    }
}
