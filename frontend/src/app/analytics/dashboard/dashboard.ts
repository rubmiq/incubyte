import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { catchError, merge, of, startWith, Subject, switchMap } from 'rxjs';

import { AnalyticsService, AnalyticsFilters, DashboardResponse } from '../analytics.service';
import { COUNTRIES, COUNTRY_LABELS, Country, DEPARTMENTS, DEPARTMENT_LABELS, Department } from '../../employees/employee-reference-data';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatFormFieldModule,
    MatSelectModule,
    RouterLink,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly analyticsService = inject(AnalyticsService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload = new Subject<void>();

  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly dashboard = signal<DashboardResponse | null>(null);

  protected readonly countryControl = new FormControl<Country | ''>('', { nonNullable: true });
  protected readonly departmentControl = new FormControl<Department | ''>('', { nonNullable: true });

  protected readonly countries = COUNTRIES;
  protected readonly countryLabels = COUNTRY_LABELS;
  protected readonly departments = DEPARTMENTS;
  protected readonly departmentLabels = DEPARTMENT_LABELS;

  protected readonly countryColumns = ['country', 'headcount', 'total_payroll', 'mean_salary', 'median_salary'];
  protected readonly departmentColumns = ['department', 'headcount', 'total_payroll', 'mean_salary', 'median_salary'];

  constructor() {
    merge(this.countryControl.valueChanges, this.departmentControl.valueChanges, this.reload)
      .pipe(
        startWith(null),
        switchMap(() => {
          this.loading.set(true);
          this.error.set(false);
          const filters: AnalyticsFilters = {};
          if (this.countryControl.value) filters.country = this.countryControl.value;
          if (this.departmentControl.value) filters.department = this.departmentControl.value;
          return this.analyticsService.getDashboard(filters).pipe(
            catchError(() => {
              this.error.set(true);
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((dashboard) => {
        this.loading.set(false);
        this.dashboard.set(dashboard);
      });
  }

  protected retry(): void {
    this.reload.next();
  }

  protected clearFilters(): void {
    this.countryControl.setValue('', { emitEvent: false });
    this.departmentControl.setValue('', { emitEvent: false });
    this.reload.next();
  }

  protected formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(value);
  }

  protected getCountryEntries(): [string, any][] {
    const dashboard = this.dashboard();
    return dashboard ? Object.entries(dashboard.by_country) : [];
  }

  protected getDepartmentEntries(): [string, any][] {
    const dashboard = this.dashboard();
    return dashboard ? Object.entries(dashboard.by_department) : [];
  }
}
