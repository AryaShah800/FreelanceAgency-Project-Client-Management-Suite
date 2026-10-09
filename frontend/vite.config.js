import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VITE_');
  if (mode === 'production') {
    for (const name of ['VITE_PUBLIC_SITE_URL', 'VITE_LEGAL_OPERATOR', 'VITE_LEGAL_JURISDICTION', 'VITE_SUPPORT_EMAIL']) {
      if (!env[name]?.trim()) throw new Error(`${name} must be configured for a production build.`);
    }
    try {
      const siteUrl = new URL(env.VITE_PUBLIC_SITE_URL);
      if (siteUrl.protocol !== 'https:' || siteUrl.pathname !== '/' || siteUrl.search || siteUrl.hash) {
        throw new Error('VITE_PUBLIC_SITE_URL must be an HTTPS origin without a path.');
      }
    } catch (error) {
      if (error instanceof TypeError) throw new Error('VITE_PUBLIC_SITE_URL must be an HTTPS origin.');
      throw error;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(env.VITE_SUPPORT_EMAIL.trim())) {
      throw new Error('VITE_SUPPORT_EMAIL must be a valid monitored support address.');
    }
    for (const [name, value] of Object.entries({
      VITE_API_BASE_URL: env.VITE_API_BASE_URL,
      VITE_WS_URL: env.VITE_WS_URL,
    })) {
      const endpoint = value?.trim();
      if (!endpoint) continue;
      if (endpoint.startsWith('/') && !endpoint.startsWith('//')) continue;

      try {
        const url = new URL(endpoint);
        const localHost = ['localhost', '127.0.0.1', '::1'].includes(url.hostname.toLowerCase());
        const allowedProtocols = name === 'VITE_API_BASE_URL' ? ['https:'] : ['https:', 'wss:'];
        if (localHost || !allowedProtocols.includes(url.protocol)) {
          throw new Error(`${name} must use a deployed HTTPS/WSS endpoint in production builds.`);
        }
      } catch (error) {
        if (error instanceof TypeError) {
          throw new Error(`${name} must be an absolute secure URL or a same-origin path.`, { cause: error });
        }
        throw error;
      }
    }
  }

  return {
    plugins: [react(), tailwindcss(), {
      name: 'site-meta-origin',
      transformIndexHtml(html) {
        const siteUrl = env.VITE_PUBLIC_SITE_URL?.trim().replace(/\/+$/, '');
        return siteUrl ? html.replaceAll('%VITE_PUBLIC_SITE_URL%', siteUrl) : html;
      },
    }],
    define: {
      global: 'window',
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/ws': {
          target: 'http://localhost:8080',
          ws: true,
        }
      }
    }
  };
});
