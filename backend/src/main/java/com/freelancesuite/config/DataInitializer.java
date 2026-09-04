package com.freelancesuite.config;

import com.freelancesuite.entity.*;
import com.freelancesuite.entity.enums.*;
import com.freelancesuite.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AgencyRepository agencies; private final AppUserRepository users; private final ClientRepository clients;
    private final ProjectRepository projects; private final TaskRepository tasks; private final TaskCommentRepository comments;
    private final TimeEntryRepository timeEntries; private final InvoiceRepository invoices; private final PaymentRepository payments;
    private final ProposalRepository proposals;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AgencyRepository agencies, AppUserRepository users, ClientRepository clients, ProjectRepository projects,
                           TaskRepository tasks, TaskCommentRepository comments, TimeEntryRepository timeEntries,
                           InvoiceRepository invoices, PaymentRepository payments, ProposalRepository proposals, PasswordEncoder passwordEncoder) {
        this.agencies = agencies; this.users = users; this.clients = clients; this.projects = projects; this.tasks = tasks;
        this.comments = comments; this.timeEntries = timeEntries; this.invoices = invoices; this.payments = payments;
        this.proposals = proposals; this.passwordEncoder = passwordEncoder;
    }

    @Override @Transactional
    public void run(String... args) {
        if (users.count() > 0) return;

        String pass = passwordEncoder.encode("password123");
        Agency agency = agencies.save(Agency.builder().name("Apex Digital Solutions").gstin("27AAACA0000A1Z5").build());

        AppUser alex = user("Alex Mercer", "owner@agency.com", Role.OWNER, 200.0, agency, pass);
        AppUser priya = user("Priya Sharma", "priya@agency.com", Role.MEMBER, 150.0, agency, pass);
        AppUser daniel = user("Daniel Kim", "daniel@agency.com", Role.MEMBER, 125.0, agency, pass);
        AppUser sarah = user("Sarah Jenkins", "sarah@fintech.io", Role.CLIENT, 0.0, agency, pass);

        Client fintech = client("FinTech Innovations", "Sarah Jenkins", "sarah@fintech.io", "+1-555-0192", "27BBBCC1111B1Z2", DealStage.WON, agency);
        Client retail = client("OmniRetail Group", "Marcus Vance", "marcus@omniretail.com", "+1-555-0144", "29CCCDD2222C1Z3", DealStage.PROPOSAL_SENT, agency);
        Client health = client("PulseHealth Inc", "Elena Rostova", "elena@pulsehealth.org", "+1-555-0188", "27DDDEE3333D1Z4", DealStage.CONTACTED, agency);

        Project banking = project("Banking Portal Integration", "Custom Spring Boot Microservices Backend & React Glassmorphic Dashboard with Real-time WebSockets", new BigDecimal("450000.00"), LocalDate.now().plusMonths(2), fintech);
        Project mobile = project("Mobile Commerce App", "Flutter cross-platform app with Razorpay gateway", new BigDecimal("320000.00"), LocalDate.now().plusMonths(3), retail);

        Task schema = task("Design Database Schema", "Define JPA entities and Liquibase migrations", TaskStatus.DONE, 24.0, 20.0, banking, alex);
        Task security = task("Implement Spring Security 6", "JWT Auth filter, CORS, stateless session", TaskStatus.IN_PROGRESS, 30.0, 18.0, banking, daniel);
        Task dashboard = task("React Kanban & WebSocket Live Sync", "STOMP endpoints for live task comment broadcast", TaskStatus.IN_PROGRESS, 40.0, 12.0, banking, priya);
        Task wireframes = task("UI/UX Wireframes Sign-off", "Figma mockups for mobile checkout", TaskStatus.DONE, 20.0, 16.0, mobile, priya);
        Task catalogue = task("Product Catalogue API", "REST endpoints for product search", TaskStatus.TODO, 35.0, 0.0, mobile, daniel);

        comments.saveAll(List.of(
            TaskComment.builder().task(schema).author(alex).content("Liquibase migrations applied cleanly on dev.").createdAt(LocalDateTime.now().minusDays(5)).build(),
            TaskComment.builder().task(security).author(daniel).content("JWT filter ready. Testing endpoint authorization rules.").createdAt(LocalDateTime.now().minusDays(2)).build()
        ));

        Invoice paidBanking = invoice("INV-2026-001", new BigDecimal("150000.00"), new BigDecimal("13500.00"), new BigDecimal("13500.00"), BigDecimal.ZERO, new BigDecimal("177000.00"), InvoiceStatus.PAID, LocalDate.now().minusDays(10), banking);
        Invoice pendingRetail = invoice("INV-2026-002", new BigDecimal("100000.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("18000.00"), new BigDecimal("118000.00"), InvoiceStatus.SENT, LocalDate.now().plusDays(14), mobile);

        payments.save(Payment.builder().invoice(paidBanking).amount(paidBanking.getTotalAmount()).razorpayPaymentId("pay_demo_123456").razorpayOrderId("order_demo_654321").paidAt(LocalDateTime.now().minusDays(8)).build());

        LocalDateTime now = LocalDateTime.now();
        timeEntries.saveAll(List.of(entry(schema, alex, now.minusDays(8).withHour(10).withMinute(0), 300, true, paidBanking), entry(schema, alex, now.minusDays(7).withHour(10).withMinute(0), 330, true, paidBanking), entry(security, daniel, now.minusDays(2).withHour(9).withMinute(30), 240, false, null), entry(wireframes, priya, now.minusDays(3).withHour(10).withMinute(0), 270, false, null), entry(catalogue, daniel, now.minusDays(1).withHour(11).withMinute(0), 210, false, null), entry(dashboard, priya, now.withHour(9).withMinute(0), 180, false, null)));

        // Demo public-portal proposal — seeded as a real Proposal row
        proposals.save(Proposal.builder()
                .shareToken("demo-proposal-token-2026")
                .agency(agency)
                .clientName(fintech.getCompanyName())
                .projectScope("Spring Boot Banking REST APIs, OpenPDF GST Invoices & React Dashboard")
                .deliverables("- Milestone 1: Core Spring Boot Security & JWT\n- Milestone 2: STOMP WebSockets & Live Kanban\n- Milestone 3: Razorpay Payment Gateway & OpenPDF Streamer")
                .paymentTerms("50% advance deposit upon signing, 50% upon final acceptance")
                .estimatedBudget(new BigDecimal("450000.00"))
                .expiresAt(now.plusDays(30))
                .build());
    }

    private AppUser user(String name, String email, Role role, double rate, Agency agency, String password) { return users.save(AppUser.builder().agency(agency).name(name).email(email).role(role).hourlyRate(rate).passwordHash(password).build()); }
    private Client client(String company, String contact, String email, String phone, String gstin, DealStage stage, Agency agency) { return clients.save(Client.builder().agency(agency).companyName(company).contactPerson(contact).email(email).phone(phone).gstin(gstin).dealStage(stage).build()); }
    private Project project(String title, String desc, BigDecimal budget, LocalDate deadline, Client client) { return projects.save(Project.builder().client(client).title(title).description(desc).budget(budget).deadline(deadline).build()); }
    private Task task(String title, String desc, TaskStatus status, double est, double act, Project project, AppUser assignee) { return tasks.save(Task.builder().project(project).assignedTo(assignee).title(title).description(desc).status(status).estimatedHours(est).actualHours(act).build()); }
    private Invoice invoice(String num, BigDecimal sub, BigDecimal cgst, BigDecimal sgst, BigDecimal igst, BigDecimal total, InvoiceStatus status, LocalDate due, Project project) { return invoices.save(Invoice.builder().project(project).invoiceNumber(num).subtotal(sub).cgst(cgst).sgst(sgst).igst(igst).totalAmount(total).status(status).dueDate(due).build()); }
    private TimeEntry entry(Task task, AppUser user, LocalDateTime start, int mins, boolean billed, Invoice invoice) { return TimeEntry.builder().task(task).user(user).startTime(start).endTime(start.plusMinutes(mins)).durationMinutes(mins).isBilled(billed).invoice(invoice).build(); }
}
