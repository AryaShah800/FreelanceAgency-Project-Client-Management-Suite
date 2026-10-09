import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    if (status >= 500 && window.Sentry?.withScope) {
      let endpoint = 'unknown';
      try {
        endpoint = new URL(error.config?.url || '', error.config?.baseURL || window.location.origin).pathname;
      } catch { /* keep the endpoint label generic */ }
      endpoint = endpoint
        .replace(/\/api\/v1\/public\/.*/, '/api/v1/public/[token-route]')
        .replace(/\/\d+(?=\/|$)/g, '/:id');
      window.Sentry.withScope((scope) => {
        scope.setTag('http.status_code', String(status));
        scope.setTag('http.method', String(error.config?.method || 'unknown').toUpperCase());
        scope.setTag('http.endpoint', endpoint.slice(0, 120));
        // Do not capture the Axios error object: it can contain request bodies,
        // reset tokens, and client-supplied business data.
        window.Sentry.captureMessage('Backend request failed', 'error');
      });
    }
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      // Stale or invalid session after server restart
      if (error.response.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
