import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, effect, inject, input, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, FormGroupDirective, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { Subscription } from 'rxjs';
import { PageResponse } from '../employees/employee.model';
import { Country } from '../employees/employee-reference-data';
import { NotificationService } from '../core/notification.service';
import { SALARY_CURRENCIES, Salary, SalaryCurrency, SalaryHistoryEntry, SalaryService } from './salary.service';

const COUNTRY_CURRENCY: Record<Country, SalaryCurrency> = {
  US: 'USD', GB: 'GBP', DE: 'EUR', FR: 'EUR', NL: 'EUR', IN: 'INR',
  BR: 'BRL', AU: 'AUD', CA: 'CAD', SG: 'SGD',
};

@Component({
  selector: 'app-salary-panel',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatCardModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatPaginatorModule],
  templateUrl: './salary-panel.html',
  styleUrl: './salary-panel.scss',
})
export class SalaryPanel {
  readonly code = input.required<string>();
  readonly country = input.required<Country>();
  readonly active = input.required<boolean>();
  private readonly service = inject(SalaryService);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formDirective = viewChild.required(FormGroupDirective);
  private currentRequest?: Subscription;
  private historyRequest?: Subscription;
  protected readonly currencies = SALARY_CURRENCIES;
  protected readonly current = signal<Salary | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal(false);
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly conflict = signal(false);
  protected readonly notice = signal<string | null>(null);
  protected readonly historyLoading = signal(true);
  protected readonly historyError = signal(false);
  protected readonly history = signal<PageResponse<SalaryHistoryEntry> | null>(null);
  protected readonly historyPage = signal(0);
  protected readonly form = new FormGroup({
    amount: new FormControl('', { nonNullable: true, validators: [Validators.required,
      Validators.pattern(/^(?=.*[1-9])(?:0|[1-9]\d{0,8})(?:\.\d{1,2})?$/)] }),
    currency: new FormControl<SalaryCurrency>('USD', { nonNullable: true, validators: [Validators.required] }),
    changeReason: new FormControl('', { nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(500)] }),
  });

  constructor() {
    effect(() => {
      const disabled = !this.active() || this.loading() || this.loadError() || this.saving();
      if (disabled && this.form.enabled) this.form.disable({ emitEvent: false });
      if (!disabled && this.form.disabled) this.form.enable({ emitEvent: false });
    });
    effect(() => {
      const code = this.code();
      const currency = COUNTRY_CURRENCY[this.country()];
      this.current.set(null);
      this.form.reset({ amount: '', currency, changeReason: '' });
      this.conflict.set(false);
      this.saveError.set(null);
      this.notice.set(null);
      this.history.set(null);
      this.historyPage.set(0);
      this.loadCurrent(code);
      this.loadHistory(code, 0);
    });
  }

  protected loadCurrent(code = this.code(), preserveDraft = false): void {
    this.currentRequest?.unsubscribe();
    this.loading.set(true);
    this.loadError.set(false);
    this.currentRequest = this.service.current(code).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: salary => {
        this.current.set(salary);
        this.loading.set(false);
        if (!preserveDraft && salary) {
          this.form.reset({ amount: salary.amount, currency: salary.currency, changeReason: '' });
        }
        if (preserveDraft) {
          this.conflict.set(false);
          this.saveError.set(null);
          this.notice.set('Current salary refreshed. Your draft is unchanged. Review it before saving.');
        }
      },
      error: () => { this.loading.set(false); this.loadError.set(true); },
    });
  }

  protected save(): void {
    if (this.saving() || this.loading() || this.loadError() || this.conflict() || !this.active()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.saving.set(true);
    this.saveError.set(null);
    this.notice.set(null);
    const draft = this.form.getRawValue();
    this.form.disable();
    this.service.save(this.code(), { ...draft, changeReason: draft.changeReason.trim(),
      expectedVersion: this.current()?.version ?? null,
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: salary => {
        this.form.enable();
        this.saving.set(false);
        this.current.set(salary);
        // Clear submitted state as well as controls so the next required reason
        // stays neutral until the user starts another change.
        this.formDirective().resetForm({ amount: salary.amount, currency: salary.currency, changeReason: '' });
        this.notice.set('Salary saved. The change has been added to history.');
        this.notifications.success('Salary saved.');
        this.historyPage.set(0);
        this.loadHistory(this.code(), 0);
      },
      error: (error: HttpErrorResponse) => {
        this.form.enable();
        this.saving.set(false);
        if (error.status === 409) {
          this.conflict.set(true);
          this.saveError.set('This salary or employee status changed. Refresh the current salary and review your draft.');
          this.notifications.error('Salary could not be saved. Refresh and review your draft.');
          return;
        }
        this.saveError.set('Could not save salary. Your draft is preserved. Please try again.');
        this.notifications.error('Could not save salary. Your draft is preserved.');
        const body: unknown = error.error;
        if (typeof body === 'object' && body !== null && 'fieldErrors' in body && Array.isArray(body.fieldErrors)) {
          for (const field of body.fieldErrors) {
            if (typeof field === 'object' && field !== null && 'field' in field && 'message' in field
              && typeof field.field === 'string' && typeof field.message === 'string') {
              this.form.get(field.field)?.setErrors({ server: field.message });
            }
          }
        }
      },
    });
  }

  protected loadHistory(code = this.code(), page = this.historyPage()): void {
    this.historyRequest?.unsubscribe();
    this.historyLoading.set(true);
    this.historyError.set(false);
    this.historyRequest = this.service.history(code, page).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: history => { this.history.set(history); this.historyLoading.set(false); },
      error: () => { this.historyLoading.set(false); this.historyError.set(true); },
    });
  }

  protected changePage(event: PageEvent): void {
    this.historyPage.set(event.pageIndex);
    this.loadHistory();
  }
}
