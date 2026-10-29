import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { SalaryPanel } from './salary-panel';
import { Salary, SalaryHistoryEntry } from './salary.service';

const URL = '/api/employees/EMP-1/salary';
const salary: Salary = { amount: '100.01', currency: 'USD', version: 0,
  effectiveFrom: '2026-10-29', updatedAt: '2026-10-29T08:00:00Z' };
const entry: SalaryHistoryEntry = { id: 1, previousAmount: null, previousCurrency: null,
  amount: '100.01', currency: 'USD', actor: null, changeReason: 'Initial seeded salary',
  effectiveFrom: '2026-10-29', createdAt: '2026-10-29T08:00:00Z' };

describe('Salary workflow', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<SalaryPanel>;
  let element: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [SalaryPanel], providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SalaryPanel);
    fixture.componentRef.setInput('code', 'EMP-1');
    fixture.componentRef.setInput('country', 'US');
    fixture.componentRef.setInput('active', true);
    element = fixture.nativeElement;
  });
  afterEach(() => http.verify());

  function history(total = 1, page = 0): void {
    http.expectOne(req => req.url === URL + '/history' && req.params.get('page') === String(page))
      .flush({ content: total ? [entry] : [], page, size: 20, totalElements: total, totalPages: Math.ceil(total / 20) });
  }
  function start(total = 1): void {
    fixture.detectChanges();
    http.expectOne(URL).flush(salary);
    history(total);
    fixture.detectChanges();
  }
  function input(name: string): HTMLInputElement | HTMLTextAreaElement {
    const control = element.querySelector<HTMLInputElement | HTMLTextAreaElement>(`[formControlName="${name}"]`);
    if (!control) throw new Error(`Missing ${name} control`);
    return control;
  }
  function fill(amount = '120.99', reason = 'Annual review'): void {
    input('amount').value = amount;
    input('amount').dispatchEvent(new Event('input'));
    input('changeReason').value = reason;
    input('changeReason').dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }
  function submit(): void {
    element.querySelector('form')?.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }
  function button(text: string): HTMLButtonElement {
    const found = Array.from(element.querySelectorAll('button')).find(item => item.textContent?.includes(text));
    if (!found) throw new Error(`Missing ${text} button`);
    return found;
  }

  it('shows exact annual salary and legacy history without inventing an actor', () => {
    start();
    expect(element.textContent).toContain('USD 100.01');
    expect(element.textContent).toContain('Not recorded (legacy)');
    expect(element.textContent).toContain('UTC');
    expect(input('amount').value).toBe('100.01');
  });

  it('rejects invalid amounts and blank reasons before HTTP', () => {
    start();
    fill('1.001', ' ');
    submit();
    http.expectNone(req => req.method === 'PUT');
    expect(element.textContent).toContain('Enter a positive amount');
    expect(element.textContent).toContain('Enter a reason');
    expect(button('Save salary').disabled).toBe(true);
  });

  it('sends decimal strings and version, prevents duplicate writes and refreshes history', async () => {
    start();
    fill();
    submit();
    submit();
    const request = http.expectOne(req => req.method === 'PUT');
    expect(request.request.body).toEqual({ amount: '120.99', currency: 'USD',
      changeReason: 'Annual review', expectedVersion: 0 });
    expect(button('Saving').disabled).toBe(true);
    request.flush({ ...salary, amount: '120.99', version: 1 });
    history(2);
    fixture.detectChanges();
    expect(element.textContent).toContain('Salary saved.');
    expect(input('changeReason').value).toBe('');
    expect(input('changeReason').getAttribute('aria-invalid')).not.toBe('true');
    expect(element.querySelector('.mat-form-field-invalid')).toBeNull();
    expect(button('Save salary').disabled).toBe(true);
    const toast = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(MatSnackBarHarness);
    expect(await toast.getMessage()).toBe('Success: Salary saved.');
  });

  it('preserves a conflicting draft and requires explicit refresh before resubmission', () => {
    start();
    fill();
    submit();
    http.expectOne(req => req.method === 'PUT').flush({ code: 'CONFLICT' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(button('Save salary').disabled).toBe(true);
    expect(input('amount').value).toBe('120.99');
    expect(input('changeReason').value).toBe('Annual review');
    submit();
    http.expectNone(req => req.method === 'PUT');
    button('Refresh current salary').click();
    http.expectOne(URL).flush({ ...salary, amount: '110.00', version: 1 });
    fixture.detectChanges();
    expect(element.textContent).toContain('USD 110.00');
    expect(input('amount').value).toBe('120.99');
    http.expectNone(req => req.method === 'PUT');
    submit();
    const retry = http.expectOne(req => req.method === 'PUT');
    expect(retry.request.body.expectedVersion).toBe(1);
    retry.flush({ ...salary, amount: '120.99', version: 2 });
    history(3);
  });

  it('shows server field errors while preserving the draft', async () => {
    start();
    fill();
    submit();
    http.expectOne(req => req.method === 'PUT').flush({ code: 'VALIDATION_FAILED',
      fieldErrors: [{ field: 'amount', message: 'Enter a different salary amount or currency.' }] },
    { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    expect(input('amount').value).toBe('120.99');
    expect(element.textContent).toContain('Enter a different salary amount or currency.');
    const toast = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(MatSnackBarHarness);
    expect(await toast.getMessage()).toBe('Error: Could not save salary. Your draft is preserved.');
  });

  it('retries a failed salary load and can establish an initial salary', () => {
    fixture.detectChanges();
    http.expectOne(URL).flush({}, { status: 500, statusText: 'Error' });
    history(0);
    fixture.detectChanges();
    expect(button('Save salary').disabled).toBe(true);
    button('Retry salary').click();
    http.expectOne(URL).flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();
    expect(element.textContent).toContain('No salary recorded.');
    fill();
    submit();
    const request = http.expectOne(req => req.method === 'PUT');
    expect(request.request.body.expectedVersion).toBeNull();
    request.flush({ ...salary, amount: '120.99' });
    history();
  });

  it('pages history on the server and retries history independently', async () => {
    start(21);
    const paginator = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatPaginatorHarness);
    await paginator.goToNextPage();
    const request = http.expectOne(req => req.url.endsWith('/history') && req.params.get('page') === '1');
    expect(request.request.params.get('size')).toBe('20');
    request.flush({}, { status: 500, statusText: 'Error' });
    fixture.detectChanges();
    expect(element.textContent).toContain('USD 100.01');
    button('Retry history').click();
    history(21, 1);
    fixture.detectChanges();
    expect(element.textContent).not.toContain('Could not load salary history.');
  });

  it('keeps archived employees read-only, including the currency select', async () => {
    fixture.componentRef.setInput('active', false);
    start();
    expect(input('amount').disabled).toBe(true);
    const select = await TestbedHarnessEnvironment.loader(fixture).getHarness(MatSelectHarness);
    expect(await select.isDisabled()).toBe(true);
    submit();
    http.expectNone(req => req.method === 'PUT');
    expect(element.textContent).toContain('Archived employees are read-only.');
  });

  it('cancels pending reads when navigating to another employee', () => {
    fixture.detectChanges();
    const previousSalary = http.expectOne(URL);
    const previousHistory = http.expectOne(req => req.url.endsWith('/history'));
    fixture.componentRef.setInput('code', 'EMP-2');
    fixture.detectChanges();
    expect(previousSalary.cancelled).toBe(true);
    expect(previousHistory.cancelled).toBe(true);
    http.expectOne('/api/employees/EMP-2/salary').flush(null);
    http.expectOne(req => req.url === '/api/employees/EMP-2/salary/history')
      .flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });
});
