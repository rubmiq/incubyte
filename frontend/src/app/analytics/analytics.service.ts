import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CountryStats {
  headcount: number;
  total_payroll_usd: number;
  mean_salary_usd: number;
  median_salary_usd: number;
}

export interface DepartmentStats {
  headcount: number;
  total_payroll_usd: number;
  mean_salary_usd: number;
  median_salary_usd: number;
}

export interface DashboardResponse {
  active_headcount: number;
  total_annual_payroll_usd: number;
  mean_salary_usd: number;
  median_salary_usd: number;
  by_country: Record<string, CountryStats>;
  by_department: Record<string, DepartmentStats>;
  fx_snapshot_date: string;
  fx_snapshot_note: string;
}

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {
  private readonly http = inject(HttpClient);

  getDashboard(): Observable<DashboardResponse> {
    return this.http.get<DashboardResponse>('/api/analytics/dashboard');
  }
}
