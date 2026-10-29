import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

import { EmployeeService } from '../employee.service';
import { ApiError } from '../../core/api-error.model';
import { NotificationService } from '../../core/notification.service';
import {
  COUNTRIES,
  COUNTRY_LABELS,
  Country,
  DEPARTMENTS,
  DEPARTMENT_LABELS,
  Department,
} from '../employee-reference-data';

@Component({
  selector: 'app-employee-form',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  templateUrl: './employee-form.html',
  styleUrl: './employee-form.scss',
})
export class EmployeeForm {
  private readonly employeeService = inject(EmployeeService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  readonly code = input<string>();

  protected readonly countries = COUNTRIES;
  protected readonly countryLabels = COUNTRY_LABELS;
  protected readonly departments = DEPARTMENTS;
  protected readonly departmentLabels = DEPARTMENT_LABELS;

  protected readonly loading = signal(false);
  protected readonly submitting = signal(false);
  protected readonly loadError = signal(false);
  protected readonly submitError = signal<string | null>(null);

  protected readonly form = new FormGroup({
    firstName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    lastName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    country: new FormControl<Country | null>(null, { validators: [Validators.required] }),
    department: new FormControl<Department | null>(null, { validators: [Validators.required] }),
    jobTitle: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    hiredOn: new FormControl<Date | null>(null, { validators: [Validators.required] }),
  });

  protected get isEditMode(): boolean {
    return !!this.code();
  }

  constructor() {
    effect(() => {
      const employeeCode = this.code();
      if (!employeeCode) {
        return;
      }
      this.loading.set(true);
      this.loadError.set(false);
      this.employeeService.getByCode(employeeCode).subscribe({
        next: (employee) => {
          this.loading.set(false);
          this.form.patchValue({
            firstName: employee.firstName,
            lastName: employee.lastName,
            email: employee.email,
            country: employee.country,
            department: employee.department,
            jobTitle: employee.jobTitle,
            hiredOn: new Date(employee.hiredOn),
          });
        },
        error: () => {
          this.loading.set(false);
          this.loadError.set(true);
        },
      });
    });
  }

  protected cancel(): void {
    void this.router.navigate(['/employees']);
  }

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    if (!value.country || !value.department || !value.hiredOn) {
      return;
    }

    this.submitting.set(true);
    this.submitError.set(null);

    // Convert Date to ISO string for API
    const hiredOnString = value.hiredOn instanceof Date
      ? value.hiredOn.toISOString().split('T')[0]
      : value.hiredOn;

    const employeeCode = this.code();
    const request$ = employeeCode
      ? this.employeeService.update(employeeCode, { ...value, hiredOn: hiredOnString, country: value.country, department: value.department })
      : this.employeeService.create({ ...value, hiredOn: hiredOnString, country: value.country, department: value.department });

    request$.subscribe({
      next: (employee) => {
        this.submitting.set(false);
        this.notifications.success(employeeCode ? 'Employee updated.' : 'Employee created.');
        void this.router.navigate(['/employees', employee.employeeCode]);
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        this.applyServerError(error);
        this.notifications.error(this.submitError() ?? 'Could not save employee. Please try again.');
      },
    });
  }

  private applyServerError(error: HttpErrorResponse): void {
    const apiError = error.error as ApiError | undefined;
    if ((error.status === 400 || error.status === 422) && apiError?.fieldErrors?.length) {
      for (const fieldError of apiError.fieldErrors) {
        const control = this.form.get(fieldError.field);
        control?.setErrors({ server: fieldError.message });
      }
      this.submitError.set('Please fix the highlighted fields.');
      return;
    }
    if (error.status === 409) {
      this.submitError.set('This email address is already in use.');
      return;
    }
    this.submitError.set('Something went wrong. Please try again.');
  }
}
