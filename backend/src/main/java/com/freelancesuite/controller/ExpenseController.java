package com.freelancesuite.controller;

import com.freelancesuite.dto.ExpenseDto;
import com.freelancesuite.security.UserPrincipal;
import com.freelancesuite.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/expenses")
@PreAuthorize("hasAnyRole('OWNER', 'MEMBER')")
public class ExpenseController {

    private final ExpenseService expenseService;

    @Autowired
    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ResponseEntity<List<ExpenseDto>> getExpenses(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(expenseService.getExpensesByProject(projectId, userPrincipal.getAgencyId()));
    }

    @PostMapping
    public ResponseEntity<ExpenseDto> createExpense(
            @PathVariable Long projectId,
            @Valid @RequestBody ExpenseDto dto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(expenseService.createExpense(projectId, dto, userPrincipal.getAgencyId()));
    }

    @GetMapping("/profitability")
    public ResponseEntity<Map<String, Object>> getProfitability(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(expenseService.calculateProjectProfitability(projectId, userPrincipal.getAgencyId()));
    }
}
