import { defineConfig, devices } from '@playwright/test';

// Locally set E2E_CHANNEL=msedge (or chrome) to reuse an installed browser; CI installs chromium.
const channel = process.env.E2E_CHANNEL || undefined;

export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: { baseURL: 'http://localhost:5173', channel, screenshot: 'only-on-failure', trace: 'retain-on-failure' },
  projects: [
    { name: 'desktop', testIgnore: /mobile.spec.js/, use: { ...devices['Desktop Chrome'], channel } },
    { name: 'mobile', testMatch: /mobile\.spec\.js/, use: { ...devices['Pixel 7'], channel } },
  ],
  webServer: [
    {
      // Disposable H2 demo workspace: no database, email or payment keys needed.
      command: 'mvn -B -ntp -f ../backend/pom.xml -DskipTests -Dspring-boot.run.profiles=demo spring-boot:run',
      url: 'http://localhost:8080/actuator/health/liveness',
      timeout: 240_000,
      reuseExistingServer: true,
      gracefulShutdown: { signal: 'SIGTERM', timeout: 5000 },
    },
    {
      command: 'npm run dev -- --port 5173 --strictPort',
      cwd: '../frontend',
      env: { VITE_DEMO_ACCESS_ENABLED: 'true' },
      url: 'http://localhost:5173',
      timeout: 120_000,
      reuseExistingServer: true,
    },
  ],
});
