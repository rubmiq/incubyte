import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormGroupDirective } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { provideRouter, Router } from '@angular/router';
import { EmployeeForm } from './employee-form';

const employee = { employeeCode: 'EMP-1', firstName: 'Test', lastName: 'Person',
  email: 'test@example.test', country: 'US', department: 'ENGINEERING', jobTitle: 'Engineer',
  status: 'ACTIVE', hiredOn: '2026-01-01', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' };

describe('Employee save notifications', () => {
  let fixture: ComponentFixture<EmployeeForm>;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [EmployeeForm],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EmployeeForm);
  });
  afterEach(() => http.verify());

  function start(edit = false): void {
    if (edit) fixture.componentRef.setInput('code', 'EMP-1');
    fixture.detectChanges();
    if (edit) http.expectOne('/api/employees/EMP-1').flush(employee);
    fixture.detectChanges();
    const directive = fixture.debugElement.query(By.directive(FormGroupDirective)).injector.get(FormGroupDirective);
    directive.form.setValue({ firstName: employee.firstName, lastName: employee.lastName,
      email: employee.email, country: employee.country, department: employee.department,
      jobTitle: employee.jobTitle, hiredOn: new Date('2026-01-01T00:00:00Z') });
    const element: HTMLElement = fixture.nativeElement;
    element.querySelector('form')?.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }

  it.each([{ edit: false, method: 'POST', message: 'Success: Employee created.' },
    { edit: true, method: 'PUT', message: 'Success: Employee updated.' }])(
    'shows $message only after the API succeeds', async ({ edit, method, message }) => {
      start(edit);
      const loader = TestbedHarnessEnvironment.documentRootLoader(fixture);
      expect(await loader.getAllHarnesses(MatSnackBarHarness)).toHaveLength(0);
      http.expectOne(req => req.method === method).flush(employee);
      fixture.detectChanges();
      expect(await (await loader.getHarness(MatSnackBarHarness)).getMessage()).toBe(message);
    });

  it('shows a create conflict error and preserves employee input', async () => {
    start();
    http.expectOne(req => req.method === 'POST').flush({ code: 'CONFLICT' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    const toast = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(MatSnackBarHarness);
    expect(await toast.getMessage()).toBe('Error: This email address is already in use.');
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector<HTMLInputElement>('[formControlName="email"]')?.value).toBe(employee.email);
  });

  it('shows update validation errors in the toast and beside the field', async () => {
    start(true);
    http.expectOne(req => req.method === 'PUT').flush({ code: 'VALIDATION_FAILED',
      fieldErrors: [{ field: 'email', message: 'Enter a valid work email.' }] }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    const toast = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(MatSnackBarHarness);
    expect(await toast.getMessage()).toBe('Error: Please fix the highlighted fields.');
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Enter a valid work email.');
  });
});
