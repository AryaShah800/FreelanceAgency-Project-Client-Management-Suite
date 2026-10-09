package com.freelancesuite.service;

import com.freelancesuite.dto.ExpenseDto;
import com.freelancesuite.entity.Expense;
import com.freelancesuite.entity.Project;
import com.freelancesuite.entity.TimeEntry;
import com.freelancesuite.repository.ExpenseRepository;
import com.freelancesuite.repository.ProjectRepository;
import com.freelancesuite.repository.TimeEntryRepository;
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
    private final TimeEntryRepository timeEntryRepository;

    @Autowired
    public ExpenseService(ExpenseRepository expenseRepository, ProjectRepository projectRepository, TimeEntryRepository timeEntryRepository) {
        this.expenseRepository = expenseRepository;
        this.projectRepository = projectRepository;
        this.timeEntryRepository = timeEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<ExpenseDto> getExpensesByProject(Long projectId, Long agencyId) {
        projectRepository.findByIdAndAgencyId(projectId, agencyId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("You cannot access this project's expenses"));
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

    @Transactional(readOnly = true)
    public Map<String, Object> calculateProjectProfitability(Long projectId, Long agencyId) {
        Project project = projectRepository.findByIdAndAgencyId(projectId, agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

        BigDecimal revenue = project.getBudget() != null ? project.getBudget() : BigDecimal.ZERO;
        List<Expense> expenses = expenseRepository.findByProjectId(projectId);
        BigDecimal hardExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Fix: Calculate Labor Costs from logged time
        BigDecimal laborCosts = BigDecimal.ZERO;
        List<TimeEntry> timeEntries = timeEntryRepository.findByTaskProjectId(project.getId());
        for (TimeEntry te : timeEntries) {
            if (te.getEndTime() != null) {
                double hours = te.getDurationMinutes() / 60.0;
                double rate = (te.getUser() != null && te.getUser().getHourlyRate() != null)
                        ? te.getUser().getHourlyRate() : 100.0; // Default rate if none
                laborCosts = laborCosts.add(BigDecimal.valueOf(hours * rate));
            }
        }

        BigDecimal totalExpenses = hardExpenses.add(laborCosts.setScale(2, java.math.RoundingMode.HALF_UP));

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
