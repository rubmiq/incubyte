import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { PageResponse } from '../employees/employee.model';

export const SALARY_CURRENCIES = ['USD', 'GBP', 'EUR', 'INR', 'BRL', 'AUD', 'CAD', 'SGD'] as const;
export type SalaryCurrency = (typeof SALARY_CURRENCIES)[number];

export interface Salary {
  readonly amount: string;
  readonly currency: SalaryCurrency;
  readonly version: number;
  readonly effectiveFrom: string;
  readonly updatedAt: string;
}

export interface SalaryHistoryEntry {
  readonly id: number;
  readonly previousAmount: string | null;
  readonly previousCurrency: string | null;
  readonly amount: string;
  readonly currency: string;
  readonly actor: string | null;
  readonly changeReason: string;
  readonly effectiveFrom: string;
  readonly createdAt: string;
}

export interface SaveSalaryRequest {
  readonly amount: string;
  readonly currency: SalaryCurrency;
  readonly changeReason: string;
  readonly expectedVersion: number | null;
}

@Injectable({ providedIn: 'root' })
export class SalaryService {
  private readonly http = inject(HttpClient);
  private url(code: string): string {
    return `/api/employees/${encodeURIComponent(code)}/salary`;
  }
  current(code: string) {
    return this.http.get<Salary | null>(this.url(code));
  }
  save(code: string, request: SaveSalaryRequest) {
    return this.http.put<Salary>(this.url(code), request);
  }
  history(code: string, page: number) {
    return this.http.get<PageResponse<SalaryHistoryEntry>>(`${this.url(code)}/history`, {
      params: { page, size: 20 },
    });
  }
}
