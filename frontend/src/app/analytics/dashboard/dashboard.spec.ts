import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { provideRouter } from '@angular/router';
import { Dashboard } from './dashboard';

describe('Analytics filters', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('renders filtered summary and breakdown tables', () => {
    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();
    const stats = { headcount: 1, total_payroll_usd: 100, mean_salary_usd: 100, median_salary_usd: 100 };
    http.expectOne('/api/analytics/dashboard').flush({
      active_headcount: 1, total_annual_payroll_usd: 100, mean_salary_usd: 100,
      median_salary_usd: 100, by_country: { US: stats },
      by_department: { ENGINEERING: stats }, fx_snapshot_date: '2026-01-01',
      fx_snapshot_note: 'Demo FX',
    });
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelectorAll('th').length).toBe(10);
    expect(element.querySelectorAll('tr.mat-mdc-row').length).toBe(2);
    expect(element.textContent).toContain('$100.00');
    expect(element.textContent).toContain('US');
    expect(element.textContent).toContain('ENGINEERING');
  });

  it('combines filters, cancels older requests and clears both with one request', async () => {
    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();
    const initial = http.expectOne('/api/analytics/dashboard');
    const selects = await TestbedHarnessEnvironment.loader(fixture).getAllHarnesses(MatSelectHarness);
    await selects[0].open();
    await selects[0].clickOptions({ text: 'United States' });
    expect(initial.cancelled).toBe(true);
    const country = http.expectOne(req => req.params.get('country') === 'US');
    expect(country.request.params.has('department')).toBe(false);
    await selects[1].open();
    await selects[1].clickOptions({ text: 'Engineering' });
    expect(country.cancelled).toBe(true);
    const combined = http.expectOne(req => req.params.get('country') === 'US'
      && req.params.get('department') === 'ENGINEERING');
    const element: HTMLElement = fixture.nativeElement;
    element.querySelector<HTMLButtonElement>('.filters button')?.click();
    expect(combined.cancelled).toBe(true);
    const cleared = http.expectOne('/api/analytics/dashboard');
    expect(cleared.request.params.keys()).toEqual([]);
    cleared.flush(null);
    expect(await selects[0].getValueText()).toBe('All countries');
    expect(await selects[1].getValueText()).toBe('All departments');
  });

  it('retries after an API failure', () => {
    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();
    http.expectOne('/api/analytics/dashboard').flush({}, { status: 500, statusText: 'Error' });
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Failed to load dashboard data.');
    element.querySelector<HTMLButtonElement>('.error-container button')?.click();
    http.expectOne('/api/analytics/dashboard').flush(null);
    fixture.detectChanges();
    expect(element.textContent).not.toContain('Failed to load dashboard data.');
  });
});
