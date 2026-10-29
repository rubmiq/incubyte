import { Component, DestroyRef, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { combineLatest, catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';

import { EmployeeService } from '../employee.service';
import { EmployeeListQuery, EmployeeSummary, PageResponse } from '../employee.model';
import {
  COUNTRIES,
  COUNTRY_LABELS,
  Country,
  DEPARTMENTS,
  DEPARTMENT_LABELS,
  Department,
  EMPLOYEE_STATUSES,
  EmployeeStatus,
} from '../employee-reference-data';

@Component({
  selector: 'app-employee-list',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.scss',
})
export class EmployeeList {
  private readonly employeeService = inject(EmployeeService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly countries = COUNTRIES;
  protected readonly countryLabels = COUNTRY_LABELS;
  protected readonly departments = DEPARTMENTS;
  protected readonly departmentLabels = DEPARTMENT_LABELS;
  protected readonly statuses = EMPLOYEE_STATUSES;

  protected readonly pageSize = 20;
  protected readonly displayedColumns = [
    'employeeCode',
    'name',
    'email',
    'country',
    'department',
    'status',
    'hiredOn',
    'actions',
  ];

  private readonly initialParams = this.route.snapshot.queryParamMap;

  protected readonly searchControl = new FormControl(this.initialParams.get('search') ?? '', {
    nonNullable: true,
  });
  protected readonly countryControl = new FormControl<Country | ''>(
    (this.initialParams.get('country') as Country | null) ?? '',
    { nonNullable: true },
  );
  protected readonly departmentControl = new FormControl<Department | ''>(
    (this.initialParams.get('department') as Department | null) ?? '',
    { nonNullable: true },
  );
  protected readonly statusControl = new FormControl<EmployeeStatus | ''>(
    (this.initialParams.get('status') as EmployeeStatus | null) ?? '',
    { nonNullable: true },
  );

  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly actionError = signal<string | null>(null);
  protected readonly actionPending = signal(false);
  protected readonly result = signal<PageResponse<EmployeeSummary> | null>(null);
  protected readonly pageIndex = signal(Number(this.initialParams.get('page') ?? 0));
  protected readonly sortActive = signal(this.initialParams.get('sortField') ?? 'lastName');
  protected readonly sortDirection = signal<'asc' | 'desc'>(
    (this.initialParams.get('sortDirection') as 'asc' | 'desc' | null) ?? 'asc',
  );

  private readonly reloadTrigger = signal(0);

  constructor() {
    this.searchControl.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.applyFilters());

    this.countryControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.applyFilters());
    this.departmentControl.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.applyFilters());
    this.statusControl.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.applyFilters());

    combineLatest([this.route.queryParamMap, toObservable(this.reloadTrigger)])
      .pipe(
        switchMap(([params]) => {
          this.loading.set(true);
          this.error.set(false);
          return this.employeeService.list(this.queryFromParams(params)).pipe(
            catchError(() => {
              this.error.set(true);
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((page) => {
        this.loading.set(false);
        this.result.set(page);
      });
  }

  private queryFromParams(params: ParamMap): EmployeeListQuery {
    this.searchControl.setValue(params.get('search') ?? '', { emitEvent: false });
    this.countryControl.setValue((params.get('country') as Country | null) ?? '', { emitEvent: false });
    this.departmentControl.setValue((params.get('department') as Department | null) ?? '', { emitEvent: false });
    this.statusControl.setValue((params.get('status') as EmployeeStatus | null) ?? '', { emitEvent: false });
    const page = Number(params.get('page') ?? 0);
    const sortField = params.get('sortField') ?? 'lastName';
    const sortDirection = (params.get('sortDirection') as 'asc' | 'desc' | null) ?? 'asc';
    this.pageIndex.set(page);
    this.sortActive.set(sortField);
    this.sortDirection.set(sortDirection);
    return {
      search: params.get('search') || undefined,
      country: (params.get('country') as Country | null) || undefined,
      department: (params.get('department') as Department | null) || undefined,
      status: (params.get('status') as EmployeeStatus | null) || undefined,
      page,
      size: this.pageSize,
      sort: `${sortField},${sortDirection}`,
    };
  }

  private applyFilters(): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        search: this.searchControl.value || null,
        country: this.countryControl.value || null,
        department: this.departmentControl.value || null,
        status: this.statusControl.value || null,
        page: 0,
      },
      queryParamsHandling: 'merge',
    });
  }

  protected onPage(event: PageEvent): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { page: event.pageIndex },
      queryParamsHandling: 'merge',
    });
  }

  protected onSort(sort: Sort): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        sortField: sort.direction ? sort.active : 'lastName',
        sortDirection: sort.direction || 'asc',
        page: 0,
      },
      queryParamsHandling: 'merge',
    });
  }

  protected archive(employeeCode: string): void {
    this.changeStatus(employeeCode, true);
  }

  protected reactivate(employeeCode: string): void {
    this.changeStatus(employeeCode, false);
  }

  protected retry(): void {
    this.reloadTrigger.update((value) => value + 1);
  }

  protected countryLabel(employee: EmployeeSummary): string {
    return this.countryLabels[employee.country];
  }

  protected departmentLabel(employee: EmployeeSummary): string {
    return this.departmentLabels[employee.department];
  }

  private changeStatus(employeeCode: string, archive: boolean): void {
    if (this.actionPending()) return;
    this.actionPending.set(true);
    this.actionError.set(null);
    const request = archive
      ? this.employeeService.archive(employeeCode)
      : this.employeeService.reactivate(employeeCode);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.actionPending.set(false);
        this.retry();
      },
      error: () => {
        this.actionPending.set(false);
        this.actionError.set('Could not change employee status. Refresh the directory and try again.');
      },
    });
  }
}
