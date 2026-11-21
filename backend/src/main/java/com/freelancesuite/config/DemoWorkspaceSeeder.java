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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the disposable "demo" workspace: a small, believable agency with a full pipeline,
 * projects at different stages, margins that are not all 99%, paid / sent / overdue / draft
 * invoices (GST split by client state) and an activity feed. Everything is fictional and
 * deterministic, so counts and totals can be asserted in tests.
 */
@Component
@Profile("demo")
public class DemoWorkspaceSeeder implements CommandLineRunner {

    public static final String OWNER_EMAIL = "demo@agency.test";
    public static final String PASSWORD = "DemoAccess2026!";
    private static final String AGENCY_STATE = "27"; // Maharashtra: same-state clients get CGST+SGST

    private final AgencyRepository agencies;
    private final AppUserRepository users;
    private final ClientRepository clients;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final TaskCommentRepository comments;
    private final TimeEntryRepository timeEntries;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final ProposalRepository proposals;
    private final ExpenseRepository expenses;
    private final AuditEventRepository auditEvents;
    private final PlatformStateRepository platformState;
    private final PasswordEncoder passwordEncoder;

    public DemoWorkspaceSeeder(AgencyRepository agencies, AppUserRepository users, ClientRepository clients,
                               ProjectRepository projects, TaskRepository tasks, TaskCommentRepository comments,
                               TimeEntryRepository timeEntries, InvoiceRepository invoices, PaymentRepository payments,
                               ProposalRepository proposals, ExpenseRepository expenses, AuditEventRepository auditEvents,
                               PlatformStateRepository platformState, PasswordEncoder passwordEncoder) {
        this.agencies = agencies; this.users = users; this.clients = clients; this.projects = projects;
        this.tasks = tasks; this.comments = comments; this.timeEntries = timeEntries; this.invoices = invoices;
        this.payments = payments; this.proposals = proposals; this.expenses = expenses;
        this.auditEvents = auditEvents; this.platformState = platformState; this.passwordEncoder = passwordEncoder;
    }

    // company, contact, email, state code, stage
    private static final long DEMO_HOURLY_RATE = 1500;

    private record ClientSpec(String company, String contact, String email, String state, DealStage stage) {}
    // title, description, client index, budget, target margin %, months since kickoff, weeks until deadline, task progress, invoice plan
    private record ProjectSpec(String title, String desc, int client, long budget, int margin, int monthsAgo,
                               int weeksToDeadline, String progress, String invoicePlan) {}

    private static final List<ClientSpec> CLIENTS = List.of(
            new ClientSpec("Northwind Logistics", "Meera Iyer", "meera@northwind.example", "27", DealStage.WON),
            new ClientSpec("Brightleaf Organics", "Karthik Rao", "karthik@brightleaf.example", "29", DealStage.WON),
            new ClientSpec("Kestrel Fintech", "Anjali Desai", "anjali@kestrel.example", "27", DealStage.WON),
            new ClientSpec("Lumen Dental Group", "Dr. Vikram Shah", "vikram@lumendental.example", "24", DealStage.WON),
            new ClientSpec("Orbit Mobility", "Sana Qureshi", "sana@orbitmobility.example", "29", DealStage.PROPOSAL_SENT),
            new ClientSpec("Saffron & Co. Foods", "Rohan Mehta", "rohan@saffronco.example", "27", DealStage.CONTACTED),
            new ClientSpec("Helix Diagnostics", "Dr. Priya Nair", "priya@helixdx.example", "36", DealStage.LEAD),
            new ClientSpec("Paperkite Studios", "Dev Malhotra", "dev@paperkite.example", "07", DealStage.LOST));

    // progress: D = all tasks done, M = mixed, S = just started. Invoice plan: P paid, S sent, O overdue, D draft.
    private static final List<ProjectSpec> PROJECTS = List.of(
            new ProjectSpec("Fleet Tracking Dashboard", "Live vehicle map, driver scorecards and delay alerts", 0, 620000, 38, 6, -8, "D", "PPP"),
            new ProjectSpec("Customer Support Portal", "Self-service ticketing with SLA timers", 0, 280000, 46, 3, 4, "M", "PS"),
            new ProjectSpec("Subscription Storefront", "Headless storefront with recurring orders", 1, 450000, 29, 5, -2, "D", "PP"),
            new ProjectSpec("Farm Partner Onboarding App", "Supplier KYC and produce listings", 1, 340000, 41, 2, 8, "M", "PO"),
            new ProjectSpec("Brand Microsite", "Campaign site with CMS handoff", 1, 120000, 52, 4, -6, "D", "P"),
            new ProjectSpec("Loan Origination Workflow", "Document intake and credit-officer review queue", 2, 780000, 33, 6, 6, "M", "PPS"),
            new ProjectSpec("Merchant Analytics Console", "Settlement and chargeback reporting", 2, 390000, 44, 3, 3, "M", "PS"),
            new ProjectSpec("Compliance Audit Trail", "Immutable event log and export for auditors", 2, 210000, 57, 1, 10, "S", "D"),
            new ProjectSpec("Clinic Booking Platform", "Online appointments, reminders and payments", 3, 360000, 36, 5, -3, "D", "PP"),
            new ProjectSpec("Patient Feedback Pipeline", "Post-visit surveys and sentiment tagging", 3, 150000, 48, 2, 5, "M", "PS"),
            new ProjectSpec("Branch Locator Revamp", "Maps-based branch finder with live hours", 3, 95000, 24, 1, 3, "S", "O"),
            new ProjectSpec("Internal Ops Wiki", "Searchable runbooks for the support team", 0, 80000, 61, 7, -20, "D", "P"));

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) return;
        platformState.save(new PlatformState(1, true));
        String hash = passwordEncoder.encode(PASSWORD);

        Agency agency = agencies.save(Agency.builder().name("Freelance Suite Demo").gstin("27AAACA0000A1Z5").pan("AAACA0000A")
                .registeredAddress("301 Techno Tower, Andheri East, Mumbai 400069, Maharashtra").stateCode(AGENCY_STATE)
                .bankName("HDFC Bank").bankAccountNumber("50100000000001").bankIfsc("HDFC0000001").build());
        AppUser owner = user("Demo Owner", OWNER_EMAIL, Role.OWNER, 2000.0, agency, hash);
        AppUser priya = user("Priya Sharma", "staff-one@agency.test", Role.MEMBER, 1500.0, agency, hash);
        AppUser daniel = user("Daniel Kim", "staff-two@agency.test", Role.MEMBER, 1250.0, agency, hash);
        user("Meera Iyer", "client@agency.test", Role.CLIENT, 0.0, agency, hash);
        AppUser[] team = {owner, priya, daniel};

        List<Client> savedClients = new ArrayList<>();
        for (ClientSpec c : CLIENTS) {
            savedClients.add(clients.save(Client.builder().agency(agency).companyName(c.company()).contactPerson(c.contact())
                    .email(c.email()).phone("+91-98200-0" + (1000 + savedClients.size() * 111)).stateCode(c.state())
                    .registeredAddress("Registered office, " + c.company() + ", India").dealStage(c.stage()).build()));
        }

        LocalDateTime now = LocalDateTime.now();
        int invoiceSeq = 1;
        int p = 0;
        for (ProjectSpec ps : PROJECTS) {
            Client client = savedClients.get(ps.client());
            Project project = projects.save(Project.builder().client(client).title(ps.title()).description(ps.desc())
                    .budget(BigDecimal.valueOf(ps.budget())).deadline(LocalDate.now().plusWeeks(ps.weeksToDeadline()))
                    .createdAt(now.minusMonths(ps.monthsAgo()).minusDays(3)).build());

            // Labour comes from time entries, the rest of the cost from expenses,
            // so the profitability endpoint lands near the target margin.
            BigDecimal labour = seedTasks(project, ps, team, p, now);
            BigDecimal totalCost = BigDecimal.valueOf(ps.budget()).multiply(BigDecimal.valueOf(100 - ps.margin()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal hard = totalCost.subtract(labour).max(BigDecimal.valueOf(2000));
            expenses.saveAll(List.of(
                    Expense.builder().project(project).description("Contract developer").category("Contractor")
                            .amount(hard.multiply(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_UP))
                            .createdAt(now.minusMonths(ps.monthsAgo()).plusDays(10)).build(),
                    Expense.builder().project(project).description("Cloud hosting and tooling").category("Software")
                            .amount(hard.multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP))
                            .createdAt(now.minusMonths(ps.monthsAgo()).plusDays(12)).build()));

            // Invoices: equal instalments across the plan, GST split by client state.
            String plan = ps.invoicePlan();
            int instalments = plan.length() == 1 ? 2 : plan.length();
            long base = ps.budget() / instalments / 500 * 500; // whole rupees, multiples of 500
            for (int i = 0; i < plan.length(); i++) {
                char s = plan.charAt(i);
                LocalDateTime created = now.minusMonths(Math.max(0, ps.monthsAgo() - i)).minusDays(5 + i);
                InvoiceStatus status = switch (s) {
                    case 'P' -> InvoiceStatus.PAID;
                    case 'S' -> InvoiceStatus.SENT;
                    case 'O' -> InvoiceStatus.OVERDUE;
                    default -> InvoiceStatus.DRAFT;
                };
                LocalDate due = switch (s) {
                    case 'P' -> created.toLocalDate().plusDays(14);
                    case 'O' -> LocalDate.now().minusDays(9 + p);
                    case 'S' -> LocalDate.now().plusDays(10 + p);
                    default -> LocalDate.now().plusDays(21);
                };
                long amount = i == instalments - 1 ? ps.budget() - base * (instalments - 1) : base;
                Invoice inv = invoice(String.format("INV-2026-%03d", invoiceSeq++), amount, ps.title(), i + 1, instalments, client.getStateCode(), status, due, created, project);
                if (status == InvoiceStatus.PAID) {
                    payments.save(Payment.builder().invoice(inv).amount(inv.getTotalAmount())
                            .razorpayPaymentId("pay_demo_" + inv.getInvoiceNumber())
                            .razorpayOrderId("order_demo_" + inv.getInvoiceNumber())
                            .paidAt(created.plusDays(6)).build());
                }
            }
            p++;
        }

        seedProposals(agency, now);
        seedActivity(agency, now);
    }

    private BigDecimal seedTasks(Project project, ProjectSpec ps, AppUser[] team, int index, LocalDateTime now) {
        String[][] titles = {
                {"Discovery and wireframes", "Sign off scope and flows"},
                {"API and data model", "Service layer, validation, auth"},
                {"Frontend build", "Screens, states and responsive layout"}};
        BigDecimal labour = BigDecimal.ZERO;
        for (int t = 0; t < 3; t++) {
            TaskStatus status = switch (ps.progress()) {
                case "D" -> TaskStatus.DONE;
                case "S" -> t == 0 ? TaskStatus.IN_PROGRESS : TaskStatus.TODO;
                default -> t == 0 ? TaskStatus.DONE : (t == 1 ? TaskStatus.IN_PROGRESS : (index % 2 == 0 ? TaskStatus.REVIEW : TaskStatus.TODO));
            };
            AppUser who = team[(index + t) % 3];
            double scale = Math.min(1.0, ps.budget() / 450000.0); // small projects log fewer hours
            double est = Math.max(6, Math.round((24 + 8 * t) * scale));
            double act = status == TaskStatus.TODO ? 0 : est * (status == TaskStatus.DONE ? 0.9 : 0.5);
            Task task = tasks.save(Task.builder().project(project).assignedTo(who).title(titles[t][0]).description(titles[t][1])
                    .status(status).estimatedHours(est).actualHours(act).build());
            if (act > 0) {
                int mins = (int) Math.round(act * 60 / 4.0);
                for (int e = 0; e < 4; e++) {
                    LocalDateTime start = now.minusMonths(Math.max(0, ps.monthsAgo() - 1)).minusDays(e * 3L + t)
                            .withHour(10).withMinute(0).withSecond(0).withNano(0);
                    timeEntries.save(TimeEntry.builder().task(task).user(who).startTime(start).endTime(start.plusMinutes(mins))
                            .durationMinutes(mins).isBilled(status == TaskStatus.DONE).build());
                    labour = labour.add(BigDecimal.valueOf(mins / 60.0 * who.getHourlyRate()));
                }
            }
            if (t == 1 && status != TaskStatus.TODO) {
                comments.save(TaskComment.builder().task(task).author(who)
                        .content("Draft is up for review. Edge cases are listed in the ticket.")
                        .createdAt(now.minusDays(2 + index)).build());
            }
        }
        return labour.setScale(2, RoundingMode.HALF_UP);
    }

    private void seedProposals(Agency agency, LocalDateTime now) {
        // Awaiting the client: the public link works and can be accepted live in the demo.
        proposals.save(Proposal.builder().shareToken("demo-proposal-token-2026").agency(agency).clientName("Orbit Mobility")
                .clientEmail("sana@orbitmobility.example")
                .projectScope("Rider app, dispatch console and driver payouts for a city bike-share pilot")
                .deliverables("- Milestone 1: Rider app MVP\n- Milestone 2: Dispatch console\n- Milestone 3: Payouts and reporting")
                .paymentTerms("40% on signing, 40% at beta, 20% on launch").estimatedBudget(new BigDecimal("540000.00"))
                .expiresAt(now.plusDays(21)).build());
        proposals.save(Proposal.builder().shareToken("demo-proposal-saffron").agency(agency).clientName("Saffron & Co. Foods")
                .clientEmail("rohan@saffronco.example").projectScope("Wholesale ordering portal for 200 restaurant accounts")
                .deliverables("- Catalogue and pricing tiers\n- Repeat-order workflow\n- Invoice and GST export")
                .paymentTerms("50% advance, 50% on delivery").estimatedBudget(new BigDecimal("310000.00"))
                .expiresAt(now.plusDays(14)).build());
        Proposal accepted = Proposal.builder().shareToken("demo-proposal-lumen").agency(agency).clientName("Lumen Dental Group")
                .clientEmail("vikram@lumendental.example").projectScope("Clinic Booking Platform")
                .deliverables("- Booking engine\n- SMS reminders\n- Online payments").paymentTerms("50% advance, 50% on go-live")
                .estimatedBudget(new BigDecimal("360000.00")).expiresAt(now.plusDays(5)).isSigned(true)
                .isConvertedToProject(true).build();
        accepted.setSignatureName("Dr. Vikram Shah");
        accepted.setSignedAt(now.minusMonths(5));
        proposals.save(accepted);
        proposals.save(Proposal.builder().shareToken("demo-proposal-paperkite").agency(agency).clientName("Paperkite Studios")
                .clientEmail("dev@paperkite.example").projectScope("Portfolio platform for illustrators")
                .deliverables("- Gallery CMS\n- Licensing checkout").paymentTerms("30% advance")
                .estimatedBudget(new BigDecimal("175000.00")).expiresAt(now.minusDays(12)).build());
    }

    private void seedActivity(Agency agency, LocalDateTime now) {
        Object[][] events = {
                {"Client (Dr. Vikram Shah)", "PROPOSAL_ACCEPTED", "Lumen Dental Group accepted the Clinic Booking Platform proposal", 150 * 24},
                {"System Auto-Converter", "PROJECT_CONVERTED", "Clinic Booking Platform created with 2 milestone invoices", 150 * 24 - 1},
                {"Anjali Desai", "DEPOSIT_PAID", "Kestrel Fintech paid an instalment on Loan Origination Workflow", 9 * 24},
                {"Demo Owner", "INVOICE_SENT", "Sent an invoice to Brightleaf Organics", 6 * 24},
                {"Client (Sana Qureshi)", "PROPOSAL_VIEWED", "Orbit Mobility opened the rider app proposal", 3 * 24},
                {"Daniel Kim", "TASK_REVIEW", "Moved a task to review on Customer Support Portal", 2 * 24},
                {"System", "INVOICE_OVERDUE", "An invoice for Branch Locator Revamp is now overdue", 36},
                {"Client (Sana Qureshi)", "PROPOSAL_VIEWED", "Orbit Mobility opened the rider app proposal again", 5}};
        for (Object[] e : events) {
            auditEvents.save(AuditEvent.builder().agency(agency).actor((String) e[0]).action((String) e[1])
                    .details((String) e[2]).timestamp(now.minusHours((Integer) e[3])).build());
        }
    }

    private Invoice invoice(String number, long amount, String projectTitle, int part, int parts, String clientState, InvoiceStatus status, LocalDate due,
                            LocalDateTime created, Project project) {
        boolean sameState = AGENCY_STATE.equals(clientState);
        // Lines are built from qty x rate in whole rupees: ~70% billed as engineering hours at a flat
        // rate, the rest as a fixed-fee line, so the subtotal is exactly the sum of its lines.
        long hours = amount * 7 / 10 / DEMO_HOURLY_RATE;
        long fixedFee = amount - hours * DEMO_HOURLY_RATE;
        List<InvoiceLineItem> lines = new ArrayList<>();
        lines.add(InvoiceLineItem.builder().description(projectTitle + " - engineering (milestone " + part + " of " + parts + ")")
                .quantity((int) hours).unitPrice(BigDecimal.valueOf(DEMO_HOURLY_RATE))
                .amount(BigDecimal.valueOf(hours * DEMO_HOURLY_RATE)).build());
        lines.add(InvoiceLineItem.builder().description("Project management and QA (fixed fee)")
                .quantity(1).unitPrice(BigDecimal.valueOf(fixedFee)).amount(BigDecimal.valueOf(fixedFee)).build());
        BigDecimal subtotal = lines.stream().map(InvoiceLineItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal half = subtotal.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal igst = subtotal.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cgst = sameState ? half : BigDecimal.ZERO;
        BigDecimal sgst = sameState ? half : BigDecimal.ZERO;
        BigDecimal ig = sameState ? BigDecimal.ZERO : igst;
        Invoice invoice = Invoice.builder().project(project).invoiceNumber(number).subtotal(subtotal).cgst(cgst).sgst(sgst)
                .igst(ig).totalAmount(subtotal.add(cgst).add(sgst).add(ig)).status(status).dueDate(due).createdAt(created)
                .lineItems(lines).build();
        lines.forEach(l -> l.setInvoice(invoice));
        return invoices.save(invoice);
    }

    private AppUser user(String name, String email, Role role, double rate, Agency agency, String hash) {
        AppUser u = AppUser.builder().agency(agency).name(name).email(email).role(role).hourlyRate(rate).passwordHash(hash).build();
        u.setEmailVerified(true);
        return users.save(u);
    }
}
