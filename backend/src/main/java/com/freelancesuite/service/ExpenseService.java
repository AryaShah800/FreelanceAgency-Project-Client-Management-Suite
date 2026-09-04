package com.freelancesuite.service;

import com.freelancesuite.dto.ExpenseDto;
import com.freelancesuite.entity.Expense;
import com.freelancesuite.entity.Project;
import com.freelancesuite.repository.ExpenseRepository;
import com.freelancesuite.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final ProjectRepository projectRepository;

    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository, ProjectRepository projectRepository) {
        this.expenseRepository = expenseRepository;
        this.projectRepository = projectRepository;
    }

    public List<ExpenseDto> getExpensesByProject(Long projectId) {
        return expenseRepository.findByProjectId(projectId)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public ExpenseDto createExpense(Long projectId, ExpenseDto dto, Long agencyId) {
        Project project = projectRepository.findByIdAndAgencyId(projectId, agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        Expense expense = Expense.builder()
                .project(project)
                .description(dto.getDescription())
                .amount(dto.getAmount())
                .category(dto.getCategory() != null ? dto.getCategory() : "General")
                .build();

        expense = expenseRepository.save(expense);
        return mapToDto(expense);
    }

    public Map<String, Object> calculateProjectProfitability(Long projectId, Long agencyId) {
        Project project = projectRepository.findByIdAndAgencyId(projectId, agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        BigDecimal revenue = project.getBudget() != null ? project.getBudget() : BigDecimal.ZERO;
        List<Expense> expenses = expenseRepository.findByProjectId(projectId);
        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProfit = revenue.subtract(totalExpenses);

        // Zero-Division Guard
        BigDecimal marginPercent = BigDecimal.ZERO;
        if (revenue.compareTo(BigDecimal.ZERO) > 0) {
            marginPercent = netProfit.divide(revenue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("projectId", projectId);
        metrics.put("projectTitle", project.getTitle());
        metrics.put("revenue", revenue);
        metrics.put("totalExpenses", totalExpenses);
        metrics.put("netProfit", netProfit);
        metrics.put("marginPercent", marginPercent);

        return metrics;
    }

    private ExpenseDto mapToDto(Expense expense) {
        return ExpenseDto.builder()
                .id(expense.getId())
                .projectId(expense.getProject().getId())
                .projectTitle(expense.getProject().getTitle())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
