import React from 'react';
import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-paper px-5 py-12 text-ink">
      <section className="w-full max-w-xl rounded-2xl border border-[var(--color-border)] bg-white p-8 text-center shadow-sm sm:p-12">
        <p className="mb-3 text-sm font-semibold uppercase tracking-[0.2em] text-[var(--color-brass-dark)]">404 · Page not found</p>
        <h1 className="font-serif text-3xl font-semibold">This page isn’t here</h1>
        <p className="mx-auto mt-4 max-w-md text-[var(--color-ink-muted)]">The address may be incorrect, or the page may have moved.</p>
        <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
          <Link className="rounded-lg bg-ink px-5 py-3 font-semibold text-white" to="/">Return to the app</Link>
          <Link className="rounded-lg border border-[var(--color-border)] px-5 py-3 font-semibold" to="/contact">Contact support</Link>
        </div>
      </section>
    </main>
  );
}
