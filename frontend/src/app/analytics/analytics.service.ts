import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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

export interface AnalyticsFilters {
  country?: string;
  department?: string;
}

@Injectable({
  providedIn: 'root',
})
export class AnalyticsService {
  private readonly http = inject(HttpClient);

  getDashboard(filters?: AnalyticsFilters): Observable<DashboardResponse> {
    let params = new HttpParams();
    if (filters?.country) {
      params = params.set('country', filters.country);
    }
    if (filters?.department) {
      params = params.set('department', filters.department);
    }
    return this.http.get<DashboardResponse>('/api/analytics/dashboard', { params });
  }
}
