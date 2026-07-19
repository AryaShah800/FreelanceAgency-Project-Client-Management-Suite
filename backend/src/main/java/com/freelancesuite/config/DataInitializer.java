package com.freelancesuite.config;

import com.freelancesuite.entity.*;
import com.freelancesuite.entity.enums.*;
import com.freelancesuite.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AgencyRepository agencyRepository;
    private final AppUserRepository userRepository;
    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final InvoiceRepository invoiceRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DataInitializer(AgencyRepository agencyRepository, AppUserRepository userRepository, ClientRepository clientRepository, ProjectRepository projectRepository, TaskRepository taskRepository, InvoiceRepository invoiceRepository, PasswordEncoder passwordEncoder) {
        this.agencyRepository = agencyRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.invoiceRepository = invoiceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) return;

        // Seed Agency
        Agency agency = Agency.builder()
                .name("Apex Digital Solutions")
                .gstin("27AAAAA0000A1Z5")
                .subscriptionPlan("PRO")
                .build();
        agency = agencyRepository.save(agency);

        // Seed Owner User (Agency Owner)
        AppUser owner = AppUser.builder()
                .agency(agency)
                .name("Alex Mercer (Agency Owner)")
                .email("owner@agency.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.OWNER)
                .hourlyRate(150.0)
                .build();
        owner = userRepository.save(owner);

        // Seed Client User (Client Portal Access)
        AppUser clientUser = AppUser.builder()
                .agency(agency)
                .name("Sarah Jenkins (Client)")
                .email("sarah@fintech.io")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.CLIENT)
                .hourlyRate(0.0)
                .build();
        userRepository.save(clientUser);

        // Seed Clients Records
        Client client1 = Client.builder()
                .agency(agency)
                .companyName("FinTech Innovations")
                .contactPerson("Sarah Jenkins")
                .email("sarah@fintech.io")
                .phone("+91 98765 43210")
                .gstin("27BBBBA1111B1Z2")
                .dealStage(DealStage.WON)
                .build();
        client1 = clientRepository.save(client1);

        Client client2 = Client.builder()
                .agency(agency)
                .companyName("HealthPlus Labs")
                .contactPerson("Dr. Robert Vance")
                .email("robert@healthplus.org")
                .phone("+91 91234 56789")
                .dealStage(DealStage.PROPOSAL_SENT)
                .build();
        client2 = clientRepository.save(client2);

        // Seed Projects
        Project project1 = Project.builder()
                .client(client1)
                .title("Spring Boot Banking API & Portal")
                .description("Development of high-throughput REST APIs with Spring Security and JWT authentication")
                .budget(new BigDecimal("450000.00"))
                .deadline(LocalDate.now().plusMonths(2))
                .teamMembers(List.of(owner))
                .build();
        project1 = projectRepository.save(project1);

        // Seed Tasks
        Task task1 = Task.builder()
                .project(project1)
                .assignedTo(owner)
                .title("Design Database Schema with PostgreSQL DDL")
                .description("Create JPA Entities and relational foreign keys")
                .status(TaskStatus.DONE)
                .estimatedHours(12.0)
                .actualHours(10.5)
                .build();
        taskRepository.save(task1);

        Task task2 = Task.builder()
                .project(project1)
                .assignedTo(owner)
                .title("Configure Spring Security 6 & JWT Filter")
                .description("Stateless session handling with BCrypt hashing")
                .status(TaskStatus.IN_PROGRESS)
                .estimatedHours(8.0)
                .actualHours(4.0)
                .build();
        taskRepository.save(task2);

        // Seed Invoices
        InvoiceLineItem item1 = InvoiceLineItem.builder()
                .description("Milestone 1: Backend Architecture & JPA Schema")
                .quantity(1)
                .unitPrice(new BigDecimal("150000.00"))
                .amount(new BigDecimal("150000.00"))
                .build();

        BigDecimal subtotal = new BigDecimal("150000.00");
        BigDecimal cgst = new BigDecimal("13500.00");
        BigDecimal sgst = new BigDecimal("13500.00");
        BigDecimal total = new BigDecimal("177000.00");

        Invoice invoice = Invoice.builder()
                .project(project1)
                .invoiceNumber("INV-2026-001")
                .subtotal(subtotal)
                .cgst(cgst)
                .sgst(sgst)
                .totalAmount(total)
                .status(InvoiceStatus.PAID)
                .dueDate(LocalDate.now().plusDays(10))
                .isRecurring(false)
                .build();

        item1.setInvoice(invoice);
        invoice.setLineItems(List.of(item1));

        invoiceRepository.save(invoice);
    }
}
