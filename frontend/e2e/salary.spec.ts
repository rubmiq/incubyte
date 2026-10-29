import { expect, test } from '@playwright/test';
import type { PageResponse } from '../src/app/employees/employee.model';
import type { SalaryHistoryEntry } from '../src/app/compensation/salary.service';

test('HR establishes salary, updates it, checks audit and dashboard, and reviews a stale draft', async ({ page, context }) => {
  const username = process.env['DEMO_HR_USERNAME'];
  const password = process.env['DEMO_HR_PASSWORD'];
  if (!username || !password) throw new Error('Provide demo HR credentials through environment variables.');

  await page.goto('/login');
  await page.getByRole('textbox', { name: 'Username' }).fill(username);
  await page.getByLabel('Password', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/employees$/);
  await page.request.get('/api/auth/csrf');
  const csrf = (await context.cookies()).find(cookie => cookie.name === 'XSRF-TOKEN');
  if (!csrf) throw new Error('CSRF cookie was not issued.');
  const headers = { 'X-XSRF-TOKEN': decodeURIComponent(csrf.value) };
  await page.goto('/employees/new');
  await page.getByRole('textbox', { name: 'First name', exact: true }).fill('Salary');
  await page.getByRole('textbox', { name: 'Last name', exact: true }).fill('Browser');
  await page.getByRole('textbox', { name: 'Email', exact: true }).fill(`salary-browser-${Date.now()}@example.test`);
  await page.getByRole('combobox', { name: 'Country', exact: true }).click();
  await page.getByRole('option', { name: 'United States', exact: true }).click();
  await page.getByRole('combobox', { name: 'Department', exact: true }).click();
  await page.getByRole('option', { name: 'Engineering', exact: true }).click();
  await page.getByRole('textbox', { name: 'Job title', exact: true }).fill('Synthetic test engineer');
  await page.getByRole('textbox', { name: 'Hired on', exact: true }).fill('1/1/2026');
  const creation = page.waitForResponse(response => response.url().endsWith('/api/employees')
    && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Save', exact: true }).click();
  const created = await creation;
  expect(created.status()).toBe(201);
  const employee: { employeeCode: string } = await created.json();

  try {
    await expect(page).toHaveURL(new RegExp(`/employees/${employee.employeeCode}$`));
    await expect(page.locator('mat-snack-bar-container.toast-success').filter({ hasText: 'Success: Employee created.' })).toBeVisible();
    await page.getByRole('link', { name: 'Edit', exact: true }).click();
    await page.getByRole('textbox', { name: 'Job title', exact: true }).fill('Updated synthetic test engineer');
    await page.getByRole('button', { name: 'Save', exact: true }).click();
    await expect(page).toHaveURL(new RegExp(`/employees/${employee.employeeCode}$`));
    await expect(page.locator('mat-snack-bar-container.toast-success').filter({ hasText: 'Success: Employee updated.' })).toBeVisible();
    await page.goto('/employees');
    await page.getByRole('textbox', { name: 'Search', exact: true }).fill(employee.employeeCode);
    const row = page.getByRole('row').filter({ hasText: employee.employeeCode });
    await row.getByRole('link', { name: 'Salary Browser', exact: true }).click();
    const panel = page.locator('app-salary-panel');
    await expect(panel.getByText('No salary recorded.', { exact: false })).toBeVisible();
    await panel.getByRole('textbox', { name: 'Annual amount', exact: true }).fill('100.01');
    await panel.getByRole('textbox', { name: 'Reason for salary change', exact: true }).fill('Initial browser test salary');
    await panel.getByRole('button', { name: 'Save salary', exact: true }).click();
    await expect(panel.getByRole('status').filter({ hasText: 'Salary saved.' })).toBeVisible();
    const successToast = page.locator('mat-snack-bar-container.toast-success');
    await expect(successToast).toContainText('Success: Salary saved.');
    const bounds = await successToast.boundingBox();
    if (!bounds) throw new Error('Success toast was not visible.');
    const viewport = page.viewportSize();
    if (!viewport) throw new Error('Browser viewport was not configured.');
    expect(bounds.y).toBeLessThan(100);
    expect(bounds.x + bounds.width).toBeGreaterThan(viewport.width - 50);
    await successToast.getByRole('button', { name: 'Dismiss' }).click();
    await expect(panel.locator('tbody tr')).toHaveCount(1);

    const other = await context.newPage();
    await other.goto(`/employees/${employee.employeeCode}`);
    const draft = other.locator('app-salary-panel');
    await expect(draft.locator('.current-salary')).toContainText('USD 100.01');
    await draft.getByRole('textbox', { name: 'Annual amount', exact: true }).fill('175.00');
    await draft.getByRole('textbox', { name: 'Reason for salary change', exact: true }).fill('Preserve this draft');

    await panel.getByRole('textbox', { name: 'Annual amount', exact: true }).fill('1.001');
    await expect(panel.getByRole('button', { name: 'Save salary', exact: true })).toBeDisabled();
    await panel.getByRole('textbox', { name: 'Annual amount', exact: true }).fill('200.02');
    await panel.getByRole('textbox', { name: 'Reason for salary change', exact: true }).fill('Promotion browser test');
    await panel.getByRole('button', { name: 'Save salary', exact: true }).click();
    await expect(panel.locator('.current-salary')).toContainText('USD 200.02');
    await expect(panel.locator('tbody tr')).toHaveCount(2);

    await draft.getByRole('button', { name: 'Save salary', exact: true }).click();
    await expect(draft.getByRole('alert')).toContainText('Refresh the current salary');
    await expect(other.locator('mat-snack-bar-container.toast-error')).toContainText('Error: Salary could not be saved.');
    await expect(draft.getByRole('textbox', { name: 'Annual amount', exact: true })).toHaveValue('175.00');
    await expect(draft.getByRole('button', { name: 'Save salary', exact: true })).toBeDisabled();
    await draft.getByRole('button', { name: 'Refresh current salary', exact: true }).click();
    await expect(draft.locator('.current-salary')).toContainText('USD 200.02');
    await expect(draft.getByRole('textbox', { name: 'Annual amount', exact: true })).toHaveValue('175.00');
    await expect(draft.getByRole('button', { name: 'Save salary', exact: true })).toBeEnabled();
    await other.close();

    const auditResponse = await page.request.get(`/api/employees/${employee.employeeCode}/salary/history`);
    expect(auditResponse.ok()).toBe(true);
    const audit: PageResponse<SalaryHistoryEntry> = await auditResponse.json();
    expect(audit.totalElements).toBe(2);
    expect(audit.content[0].amount).toBe('200.02');
    expect(audit.content[0].previousAmount).toBe('100.01');
    expect(audit.content[0].changeReason).toBe('Promotion browser test');

    await page.goto('/dashboard');
    await expect(page.locator('.summary-card').filter({ hasText: 'Total Annual Payroll' })).toContainText('$200.02');
  } finally {
    const archived = await page.request.post(`/api/employees/${employee.employeeCode}/archive`, { headers });
    expect(archived.ok()).toBe(true);
  }
  await page.getByRole('button', { name: 'Account menu', exact: true }).click();
  await page.getByRole('menuitem', { name: 'Logout', exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  expect((await page.request.get('/api/auth/me')).status()).toBe(401);
  await page.goto('/employees');
  await expect(page).toHaveURL(/\/login$/);
});
