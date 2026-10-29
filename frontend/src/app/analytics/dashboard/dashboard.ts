import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { catchError, of } from 'rxjs';

import { AnalyticsService, DashboardResponse } from '../analytics.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    RouterLink,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly analyticsService = inject(AnalyticsService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly dashboard = signal<DashboardResponse | null>(null);

  protected readonly countryColumns = ['country', 'headcount', 'total_payroll', 'mean_salary', 'median_salary'];
  protected readonly departmentColumns = ['department', 'headcount', 'total_payroll', 'mean_salary', 'median_salary'];

  constructor() {
    this.loadDashboard();
  }

  private loadDashboard(): void {
    this.loading.set(true);
    this.error.set(false);

    this.analyticsService
      .getDashboard()
      .pipe(
        catchError(() => {
          this.error.set(true);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((dashboard) => {
        this.loading.set(false);
        this.dashboard.set(dashboard);
      });
  }

  protected retry(): void {
    this.loadDashboard();
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
