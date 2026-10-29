import { Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { EmployeeService } from '../employee.service';
import { EmployeeDetail } from '../employee.model';
import { COUNTRY_LABELS, DEPARTMENT_LABELS } from '../employee-reference-data';

@Component({
  selector: 'app-employee-detail',
  imports: [RouterLink, MatButtonModule, MatCardModule, MatProgressSpinnerModule],
  templateUrl: './employee-detail.html',
  styleUrl: './employee-detail.scss',
})
export class EmployeeDetailPage {
  private readonly employeeService = inject(EmployeeService);

  readonly code = input.required<string>();

  protected readonly countryLabels = COUNTRY_LABELS;
  protected readonly departmentLabels = DEPARTMENT_LABELS;

  protected readonly loading = signal(false);
  protected readonly error = signal(false);
  protected readonly employee = signal<EmployeeDetail | null>(null);
  protected readonly actionPending = signal(false);
  protected readonly actionError = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.load(this.code());
    });
  }

  private load(employeeCode: string): void {
    this.loading.set(true);
    this.error.set(false);
    this.employeeService.getByCode(employeeCode).subscribe({
      next: (employee) => {
        this.loading.set(false);
        this.employee.set(employee);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
      },
    });
  }

  protected archive(): void {
    if (this.actionPending()) return;
    this.actionError.set(null);
    this.actionPending.set(true);
    this.employeeService.archive(this.code()).subscribe({
      next: (employee) => {
        this.actionPending.set(false);
        this.employee.set(employee);
      },
      error: () => {
        this.actionPending.set(false);
        this.actionError.set('Could not archive this employee. Reload and try again.');
      },
    });
  }

  protected reactivate(): void {
    if (this.actionPending()) return;
    this.actionError.set(null);
    this.actionPending.set(true);
    this.employeeService.reactivate(this.code()).subscribe({
      next: (employee) => {
        this.actionPending.set(false);
        this.employee.set(employee);
      },
      error: () => {
        this.actionPending.set(false);
        this.actionError.set('Could not reactivate this employee. Reload and try again.');
      },
    });
  }
}
