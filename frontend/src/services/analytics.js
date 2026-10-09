const plausibleDomain = import.meta.env.VITE_PLAUSIBLE_DOMAIN?.trim();
const plausibleScriptUrl = import.meta.env.VITE_PLAUSIBLE_SCRIPT_URL?.trim();
const sentryLoaderUrl = import.meta.env.VITE_SENTRY_LOADER_URL?.trim();

export function initializeErrorTracking() {
  if (!sentryLoaderUrl || typeof document === 'undefined') return;
  if (document.querySelector('script[data-agency-error-tracking]')) return;
  const script = document.createElement('script');
  script.async = true;
  script.crossOrigin = 'anonymous';
  script.src = sentryLoaderUrl;
  script.dataset.agencyErrorTracking = 'true';
  script.onload = () => window.Sentry?.onLoad?.(() => window.Sentry.init({
    sendDefaultPii: false,
    tracesSampleRate: 0,
    beforeSend(event) {
      if (event.user) delete event.user;
      if (event.request) {
        delete event.request.query_string;
        delete event.request.data;
        if (event.request.headers) {
          delete event.request.headers.Authorization;
          delete event.request.headers.authorization;
          delete event.request.headers.Cookie;
          delete event.request.headers.cookie;
        }
        if (event.request.url) {
          try {
            const url = new URL(event.request.url, window.location.origin);
            url.search = '';
            url.hash = '';
            url.pathname = url.pathname
              .replace(/^\/p\/[^/]+\/?$/, '/p/[proposal]')
              .replace(/^\/portal\/share\/[^/]+\/?$/, '/portal/share/[token]');
            event.request.url = url.toString();
          } catch {
            event.request.url = window.location.origin;
          }
        }
      }
      for (const breadcrumb of event.breadcrumbs || []) {
        if (breadcrumb.data?.url) {
          try {
            const url = new URL(breadcrumb.data.url, window.location.origin);
            url.search = '';
            url.hash = '';
            url.pathname = url.pathname
              .replace(/^\/p\/[^/]+\/?$/, '/p/[proposal]')
              .replace(/^\/portal\/share\/[^/]+\/?$/, '/portal/share/[token]');
            breadcrumb.data.url = url.toString();
          } catch {
            delete breadcrumb.data.url;
          }
        }
        if (breadcrumb.data) {
          delete breadcrumb.data.request_body;
          delete breadcrumb.data.body;
        }
      }
      return event;
    },
  }));
  document.head.appendChild(script);
}

export function initializeAnalytics() {
  if (!plausibleDomain || !plausibleScriptUrl || typeof document === 'undefined') return;
  if (document.querySelector('script[data-agency-analytics]')) return;

  // Plausible's site-specific script URL is copied from the site's dashboard.
  // The domain and event payloads are public metadata, never an API secret.
  window.plausible = window.plausible || function (...args) {
    (window.plausible.q = window.plausible.q || []).push(args);
  };
  const script = document.createElement('script');
  script.async = true;
  script.src = plausibleScriptUrl;
  script.dataset.domain = plausibleDomain;
  script.dataset.agencyAnalytics = 'true';
  script.onload = () => window.plausible.init?.({ autoCapturePageviews: false });
  document.head.appendChild(script);
}

export function trackPageView(pathname) {
  if (!plausibleDomain || typeof window.plausible !== 'function') return;
  // Never send query strings (invite/reset tokens) or opaque public-share tokens
  // to analytics. The Plausible site script must use its manual pageview variant.
  const safePath = pathname
    .replace(/^\/p\/[^/]+\/?$/, '/p/[proposal]')
    .replace(/^\/portal\/share\/[^/]+\/?$/, '/portal/share/[token]');
  window.plausible('pageview', { url: `${window.location.origin}${safePath}` });
}

export function trackEvent(name) {
  if (!plausibleDomain || typeof window.plausible !== 'function') return;
  window.plausible(name);
}
