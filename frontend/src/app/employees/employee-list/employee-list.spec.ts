import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { EmployeeList } from './employee-list';

describe('Employee directory', () => {
  const params = new BehaviorSubject(convertToParamMap({}));
  let http: HttpTestingController;

  beforeEach(() => {
    params.next(convertToParamMap({}));
    TestBed.configureTestingModule({
      imports: [EmployeeList],
      providers: [
        provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: {
          snapshot: { queryParamMap: params.value }, queryParamMap: params.asObservable(),
        } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('shows all statuses consistently when the URL has no status filter', async () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();
    const request = http.expectOne(req => req.url === '/api/employees');
    expect(request.request.params.has('status')).toBe(false);
    request.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    const selects = await TestbedHarnessEnvironment.loader(fixture).getAllHarnesses(MatSelectHarness);
    expect(await selects[2].getValueText()).toBe('All statuses');
    expect(element.textContent).toContain('No employees match');
  });

  it('cancels stale directory requests and restores controls from navigation', () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();
    const previous = http.expectOne(req => req.url === '/api/employees');
    params.next(convertToParamMap({ search: 'Alice', status: 'ARCHIVED', page: '2' }));
    expect(previous.cancelled).toBe(true);
    const current = http.expectOne(req => req.params.get('search') === 'Alice');
    expect(current.request.params.get('status')).toBe('ARCHIVED');
    expect(current.request.params.get('page')).toBe('2');
    current.flush({ content: [], page: 2, size: 20, totalElements: 0, totalPages: 0 });
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('input')?.value).toBe('Alice');
    expect(element.textContent).toContain('ARCHIVED');
  });

  it('offers a working retry after a directory failure', () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();
    http.expectOne(req => req.url === '/api/employees')
      .flush({}, { status: 500, statusText: 'Server error' });
    fixture.detectChanges();
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('[role="alert"]')).not.toBeNull();
    element.querySelector<HTMLButtonElement>('button')?.click();
    fixture.detectChanges();
    http.expectOne(req => req.url === '/api/employees')
      .flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    fixture.detectChanges();
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });
});
