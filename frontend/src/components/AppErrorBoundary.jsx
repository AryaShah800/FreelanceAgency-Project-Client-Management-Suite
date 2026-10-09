import React from 'react';

export default class AppErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    if (import.meta.env.DEV) console.error('The application encountered a rendering error.', error, info);
    window.Sentry?.captureException?.(error, { extra: { componentStack: info.componentStack } });
  }

  render() {
    if (this.state.hasError) {
      return (
        <main className="flex min-h-screen items-center justify-center bg-paper px-5 py-12 text-ink">
          <section className="w-full max-w-xl rounded-2xl border border-[var(--color-border)] bg-white p-8 text-center shadow-sm sm:p-12">
            <p className="mb-3 text-sm font-semibold uppercase tracking-[0.2em] text-[var(--color-rust)]">500 · Something went wrong</p>
            <h1 className="font-serif text-3xl font-semibold">We couldn’t load this page</h1>
            <p className="mx-auto mt-4 max-w-md text-[var(--color-ink-muted)]">Your work is still safe. Try loading the page again, or return to the sign-in page.</p>
            <div className="mt-8 flex flex-col justify-center gap-3 sm:flex-row">
              <button className="rounded-lg bg-ink px-5 py-3 font-semibold text-white" onClick={() => window.location.reload()}>Reload page</button>
              <a className="rounded-lg border border-[var(--color-border)] px-5 py-3 font-semibold" href="/login">Go to sign in</a>
            </div>
            <a href="/contact" className="mt-6 inline-block text-sm font-medium text-[var(--color-forest)] underline">Contact support</a>
          </section>
        </main>
      );
    }
    return this.props.children;
  }
}
