package com.freelancesuite.config;

import com.freelancesuite.entity.*;
import com.freelancesuite.entity.enums.*;
import com.freelancesuite.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Creates fictional, representative records for a local demo workspace. */
@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {
    private final AgencyRepository agencies; private final AppUserRepository users; private final ClientRepository clients;
    private final ProjectRepository projects; private final TaskRepository tasks; private final TaskCommentRepository comments;
    private final TimeEntryRepository timeEntries; private final InvoiceRepository invoices; private final PaymentRepository payments;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AgencyRepository agencies, AppUserRepository users, ClientRepository clients, ProjectRepository projects,
                           TaskRepository tasks, TaskCommentRepository comments, TimeEntryRepository timeEntries,
                           InvoiceRepository invoices, PaymentRepository payments, PasswordEncoder passwordEncoder) {
        this.agencies = agencies; this.users = users; this.clients = clients; this.projects = projects; this.tasks = tasks;
        this.comments = comments; this.timeEntries = timeEntries; this.invoices = invoices; this.payments = payments; this.passwordEncoder = passwordEncoder;
    }

    @Override @Transactional
    public void run(String... args) {
        if (users.count() > 0) return;
        Agency agency = agencies.save(Agency.builder().name("Apex Digital Solutions").gstin("27AAAAA0000A1Z5").subscriptionPlan("PRO").build());
        String password = passwordEncoder.encode("password123");
        AppUser alex = user("Alex Mercer", "owner@agency.com", Role.OWNER, 150, agency, password);
        AppUser priya = user("Priya Shah", "priya@agency.com", Role.MEMBER, 95, agency, password);
        AppUser daniel = user("Daniel Kim", "daniel@agency.com", Role.MEMBER, 125, agency, password);
        AppUser sarah = user("Sarah Jenkins", "sarah@fintech.io", Role.CLIENT, 0, agency, password);
        AppUser maya = user("Maya Rao", "maya@northstar.co", Role.CLIENT, 0, agency, password);

        Client fintech = client("FinTech Innovations", "Sarah Jenkins", "sarah@fintech.io", "+91 98765 43210", "27BBBBA1111B1Z2", DealStage.WON, agency);
        Client northstar = client("Northstar Commerce", "Maya Rao", "maya@northstar.co", "+91 99887 76655", "27CCCCA2222C1Z3", DealStage.WON, agency);
        Client health = client("HealthPlus Labs", "Dr. Robert Vance", "robert@healthplus.org", "+91 91234 56789", null, DealStage.PROPOSAL_SENT, agency);
        client("Orbit Logistics", "Ishaan Mehta", "ishaan@orbitlogistics.in", "+91 90011 22334", "27DDDDA3333D1Z4", DealStage.CONTACTED, agency);
        client("Cedar & Stone", "Ava Patel", "ava@cedarstone.co", "+91 90909 80808", null, DealStage.LEAD, agency);
        client("BrightPath Education", "Noah Wilson", "noah@brightpath.edu", "+91 90123 45678", "27EEEEA4444E1Z5", DealStage.LOST, agency);

        Project banking = project(fintech, "Spring Boot Banking API & Portal", "Secure banking APIs and a customer portal with audit-ready workflows.", "450000", 60, List.of(alex, daniel));
        Project store = project(northstar, "Northstar Commerce Storefront", "Responsive commerce redesign, product discovery, and checkout improvements.", "320000", 42, List.of(alex, priya, daniel));
        Project analytics = project(fintech, "Executive Analytics Dashboard", "Operational reporting dashboard for finance and support leaders.", "180000", 75, List.of(priya, daniel));
        Project brand = project(northstar, "Brand Refresh & Design System", "Completed visual identity refresh and reusable components.", "140000", -14, List.of(priya, alex));
        Project discovery = project(health, "Patient Intake Automation Discovery", "Discovery engagement pending client approval.", "95000", 30, List.of(alex));

        Task schema = task(banking, alex, "Design PostgreSQL data model", TaskStatus.DONE, 12, 10.5, true);
        Task security = task(banking, daniel, "Configure Spring Security and JWT", TaskStatus.IN_PROGRESS, 14, 8, true);
        task(banking, daniel, "Implement audit event trail", TaskStatus.TODO, 10, 0, false);
        Task wireframes = task(store, priya, "Approve mobile checkout wireframes", TaskStatus.REVIEW, 16, 15, true);
        Task catalogue = task(store, daniel, "Build catalogue search API", TaskStatus.IN_PROGRESS, 20, 11, true);
        task(store, alex, "Prepare launch checklist", TaskStatus.TODO, 6, 0, true);
        Task dashboard = task(analytics, priya, "Create dashboard component library", TaskStatus.IN_PROGRESS, 18, 9, true);
        task(analytics, daniel, "Connect reporting data endpoints", TaskStatus.TODO, 16, 0, true);
        task(brand, priya, "Deliver colour and typography tokens", TaskStatus.DONE, 8, 8, true);
        task(brand, alex, "Run stakeholder handover", TaskStatus.DONE, 4, 4.5, true);
        task(discovery, alex, "Document discovery workshop agenda", TaskStatus.TODO, 5, 0, true);

        comments.saveAll(List.of(comment(schema, alex, "Schema review completed; the migration plan is ready."), comment(security, daniel, "Token refresh and role checks are now covered."), comment(wireframes, priya, "Updated the payment step after client feedback."), comment(wireframes, maya, "The revised mobile flow looks good to us."), comment(dashboard, priya, "First dashboard components are ready for review."), comment(catalogue, alex, "Please prioritise filters before recommendations.")));

        Invoice paidBanking = invoice(banking, "INV-2026-001", "Backend architecture & schema milestone", "150000", InvoiceStatus.PAID, -10);
        invoice(store, "INV-2026-002", "Storefront discovery and UX milestone", "96000", InvoiceStatus.SENT, 14);
        invoice(analytics, "INV-2026-003", "Analytics dashboard design sprint", "54000", InvoiceStatus.DRAFT, 28);
        Invoice paidBrand = invoice(brand, "INV-2026-004", "Brand refresh final milestone", "140000", InvoiceStatus.PAID, -28);
        payments.saveAll(List.of(payment(paidBanking, "177000", "pay_demo_banking_001"), payment(paidBrand, "165200", "pay_demo_brand_004")));

        LocalDateTime now = LocalDateTime.now();
        timeEntries.saveAll(List.of(entry(schema, alex, now.minusDays(8).withHour(10).withMinute(0), 300, true, paidBanking), entry(schema, alex, now.minusDays(7).withHour(10).withMinute(0), 330, true, paidBanking), entry(security, daniel, now.minusDays(2).withHour(9).withMinute(30), 240, false, null), entry(wireframes, priya, now.minusDays(3).withHour(10).withMinute(0), 270, false, null), entry(catalogue, daniel, now.minusDays(1).withHour(11).withMinute(0), 210, false, null), entry(dashboard, priya, now.withHour(9).withMinute(0), 180, false, null)));
    }
    private AppUser user(String name, String email, Role role, double rate, Agency agency, String password) { return users.save(AppUser.builder().agency(agency).name(name).email(email).role(role).hourlyRate(rate).passwordHash(password).build()); }
    private Client client(String company, String contact, String email, String phone, String gstin, DealStage stage, Agency agency) { return clients.save(Client.builder().agency(agency).companyName(company).contactPerson(contact).email(email).phone(phone).gstin(gstin).dealStage(stage).build()); }
    private Project project(Client client, String title, String description, String budget, int deadline, List<AppUser> members) { return projects.save(Project.builder().client(client).title(title).description(description).budget(new BigDecimal(budget)).deadline(LocalDate.now().plusDays(deadline)).teamMembers(members).build()); }
    private Task task(Project project, AppUser user, String title, TaskStatus status, double estimated, double actual, boolean visible) { return tasks.save(Task.builder().project(project).assignedTo(user).title(title).description(title + " for the current client milestone.").status(status).estimatedHours(estimated).actualHours(actual).isClientVisible(visible).build()); }
    private TaskComment comment(Task task, AppUser author, String text) { return TaskComment.builder().task(task).author(author).content(text).build(); }
    private Invoice invoice(Project project, String number, String text, String value, InvoiceStatus status, int due) { BigDecimal subtotal = new BigDecimal(value); BigDecimal tax = subtotal.multiply(new BigDecimal("0.09")); Invoice invoice = Invoice.builder().project(project).invoiceNumber(number).subtotal(subtotal).cgst(tax).sgst(tax).totalAmount(subtotal.add(tax).add(tax)).status(status).dueDate(LocalDate.now().plusDays(due)).build(); invoice.setLineItems(List.of(InvoiceLineItem.builder().invoice(invoice).description(text).quantity(1).unitPrice(subtotal).amount(subtotal).build())); return invoices.save(invoice); }
    private Payment payment(Invoice invoice, String amount, String id) { return Payment.builder().invoice(invoice).amount(new BigDecimal(amount)).razorpayPaymentId(id).razorpayOrderId("order_" + id.substring(4)).build(); }
    private TimeEntry entry(Task task, AppUser user, LocalDateTime start, int minutes, boolean billed, Invoice invoice) { return TimeEntry.builder().task(task).user(user).startTime(start).endTime(start.plusMinutes(minutes)).durationMinutes(minutes).isBilled(billed).invoice(invoice).build(); }
}
