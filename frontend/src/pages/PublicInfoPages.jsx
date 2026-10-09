import React from 'react';
import { Link } from 'react-router-dom';

const operatorName = import.meta.env.VITE_LEGAL_OPERATOR?.trim() || '[Your legal business name]';
const supportEmail = import.meta.env.VITE_SUPPORT_EMAIL?.trim();
const jurisdiction = import.meta.env.VITE_LEGAL_JURISDICTION?.trim() || '[Your country and state/province]';

function PublicLayout({ title, children }) {
  return (
    <main className="min-h-screen bg-paper px-4 py-8 text-ink sm:px-6 sm:py-12">
      <div className="mx-auto max-w-3xl">
        <nav className="mb-8 flex flex-wrap items-center gap-x-5 gap-y-2 text-sm">
          <Link className="font-semibold text-[var(--color-forest)]" to="/login">Freelance Agency Suite</Link>
          <Link to="/privacy">Privacy</Link><Link to="/terms">Terms</Link><Link to="/contact">Contact</Link>
        </nav>
        <article className="rounded-2xl border border-[var(--color-border)] bg-white p-5 shadow-sm sm:p-10">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[var(--color-brass-dark)]">Freelance Agency Suite</p>
          <h1 className="mt-2 font-serif text-3xl font-semibold sm:text-4xl">{title}</h1>
          {children}
        </article>
      </div>
    </main>
  );
}

function Section({ title, children }) {
  return <section className="mt-8"><h2 className="text-lg font-semibold">{title}</h2><div className="mt-2 space-y-3 leading-7 text-[var(--color-ink-muted)]">{children}</div></section>;
}

function OperatorNotice() {
  return <aside className="mt-6 rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm leading-6 text-amber-950"><strong>Publication checklist:</strong> replace the bracketed business and jurisdiction details, confirm the listed providers and retention practices, and have local counsel review these terms before accepting real agency data.</aside>;
}

export function PrivacyPage() {
  return <PublicLayout title="Privacy Policy">
    <p className="mt-3 text-sm text-[var(--color-ink-muted)]">Draft · Last updated: October 10, 2026</p>
    <OperatorNotice />
    <Section title="Who operates the service"><p>{operatorName} operates Freelance Agency Suite. This draft applies to information handled through the service and its related support channels.</p></Section>
    <Section title="Information the service handles"><p>Account name, email address, password hash, agency membership and role; client and team contact details; proposals, project tasks, comments, time entries, invoices, expenses and related files or records; and security or audit events needed to operate the service.</p><p>When enabled, AI proposal drafting sends the request text to Google Gemini. Payment checkout and payment status use Razorpay. Transactional account messages use Resend. The application database is hosted by the operator's configured PostgreSQL provider. Do not enter sensitive personal data into proposal or AI fields unless you are authorized to do so.</p></Section>
    <Section title="How information is used"><p>We use it to authenticate users, deliver agency workflows, send invitations and account recovery messages, generate requested drafts, process configured payments, protect the service, support customers, and meet legal obligations. If configured, Plausible Analytics receives page and funnel events. The app omits URL query values, does not send form inputs or account identifiers, and redacts opaque proposal/share tokens from tracked paths.</p></Section>
    <Section title="Sharing and retention"><p>Information is shared with infrastructure and service providers only as needed to provide enabled features, and with others when required by law or directed by the account owner. Records are retained while the account is active and as needed for security, backups, accounting, dispute resolution, and legal requirements. The operator must set and publish exact retention periods before launch.</p></Section>
    <Section title="Your choices and requests"><p>Contact the operator to request access, correction, export, or deletion, subject to applicable law and agency ownership. Agency members should also contact their agency owner. The operator must confirm the process and response times for the governing jurisdiction.</p></Section>
    <Section title="Security and changes"><p>The service uses authentication and access controls, but no internet service can promise absolute security. We may update this policy; material changes should be communicated to account owners.</p></Section>
    <Section title="Contact"><p>{supportEmail ? <a className="underline" href={`mailto:${supportEmail}`}>{supportEmail}</a> : 'Set VITE_SUPPORT_EMAIL to publish a real privacy contact address.'}</p></Section>
  </PublicLayout>;
}

export function TermsPage() {
  return <PublicLayout title="Terms of Service">
    <p className="mt-3 text-sm text-[var(--color-ink-muted)]">Draft · Last updated: October 10, 2026</p>
    <OperatorNotice />
    <Section title="Agreement and accounts"><p>These draft terms govern use of Freelance Agency Suite, operated by {operatorName}. Access is invitation-only. You must provide accurate account information, protect your credentials, and use the service only when authorized by your agency.</p></Section>
    <Section title="Agency data and permissions"><p>Agency owners and administrators control membership and permissions. You are responsible for having the rights and consents needed to upload client, staff, project, and financial information, and for reviewing who can access it.</p></Section>
    <Section title="AI-generated content"><p>AI-generated proposals and other drafts can be inaccurate or incomplete. Review and approve all content, prices, schedules, legal terms, and commitments before sharing it. AI output is not legal, tax, or professional advice.</p></Section>
    <Section title="Payments and availability"><p>If payment processing is enabled, payments are handled by the configured payment provider and may be subject to its terms and fees. We may change or suspend features to maintain or secure the service. The operator must add service levels, refund rules, fees, and cancellation terms if applicable.</p></Section>
    <Section title="Acceptable use and termination"><p>Do not misuse the service, probe accounts you do not control, upload unlawful content, or interfere with service operation. The operator may suspend access for security, legal, or material terms violations. Data export, deletion, and account termination procedures must be confirmed before launch.</p></Section>
    <Section title="Governing law and contact"><p>These terms are governed by the laws of {jurisdiction}, subject to mandatory consumer protections. Contact: {supportEmail || 'set VITE_SUPPORT_EMAIL before launch'}.</p></Section>
  </PublicLayout>;
}

export function ContactPage() {
  return <PublicLayout title="Contact support">
    <p className="mt-4 leading-7 text-[var(--color-ink-muted)]">For account access, privacy requests, or service issues, contact the service operator. Do not include passwords, authentication links, API keys, or payment card details in support messages.</p>
    <section className="mt-8 rounded-xl border border-[var(--color-border)] bg-paper p-5">
      <h2 className="font-semibold">Email support</h2>
      {supportEmail ? <a className="mt-2 inline-block break-all font-medium text-[var(--color-forest)] underline" href={`mailto:${supportEmail}`}>{supportEmail}</a> : <p className="mt-2 text-sm leading-6 text-[var(--color-rust)]">Support is not configured yet. Set VITE_SUPPORT_EMAIL to your monitored support address before launch.</p>}
    </section>
  </PublicLayout>;
}
