import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env['CI'],
  retries: 0,
  timeout: 60_000,
  use: {
    baseURL: 'http://127.0.0.1:4202',
    channel: process.env['PLAYWRIGHT_CHANNEL'] || undefined,
    // Session credentials/tokens must never enter trace or screenshot artifacts.
    trace: 'off',
    screenshot: 'off',
    video: 'off',
  },
  webServer: [{
    command: 'npm run start -- --host 127.0.0.1 --port 4202 --proxy-config e2e/proxy.conf.json',
    url: 'http://127.0.0.1:4202',
    reuseExistingServer: !process.env['CI'],
    timeout: 120_000,
  }, {
    command: process.platform === 'win32' ? 'mvnw.cmd spring-boot:run' : 'sh mvnw spring-boot:run',
    cwd: '../backend',
    url: 'http://127.0.0.1:8082/actuator/health',
    env: {
      SERVER_PORT: '8082',
      SPRING_DATASOURCE_URL: `jdbc:postgresql://${process.env['DB_HOST'] || '127.0.0.1'}:${process.env['DB_PORT'] || '5432'}/${process.env['DB_NAME'] || 'hr_e2e'}?currentSchema=salary_test_e2e`,
      SPRING_FLYWAY_SCHEMAS: 'salary_test_e2e',
    },
    reuseExistingServer: !process.env['CI'],
    timeout: 180_000,
  }],
});
