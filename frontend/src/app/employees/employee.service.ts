import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  EmployeeDetail,
  EmployeeFormValue,
  EmployeeListQuery,
  PageResponse,
  EmployeeSummary,
} from './employee.model';

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/employees';

  list(query: EmployeeListQuery): Observable<PageResponse<EmployeeSummary>> {
    let params = new HttpParams();
    if (query.search) {
      params = params.set('search', query.search);
    }
    if (query.country) {
      params = params.set('country', query.country);
    }
    if (query.department) {
      params = params.set('department', query.department);
    }
    if (query.status) {
      params = params.set('status', query.status);
    }
    if (query.page !== undefined) {
      params = params.set('page', query.page);
    }
    if (query.size !== undefined) {
      params = params.set('size', query.size);
    }
    if (query.sort) {
      params = params.set('sort', query.sort);
    }
    return this.http.get<PageResponse<EmployeeSummary>>(this.baseUrl, { params });
  }

  getByCode(employeeCode: string): Observable<EmployeeDetail> {
    return this.http.get<EmployeeDetail>(`${this.baseUrl}/${employeeCode}`);
  }

  create(request: EmployeeFormValue): Observable<EmployeeDetail> {
    return this.http.post<EmployeeDetail>(this.baseUrl, request);
  }

  update(employeeCode: string, request: EmployeeFormValue): Observable<EmployeeDetail> {
    return this.http.put<EmployeeDetail>(`${this.baseUrl}/${employeeCode}`, request);
  }

  archive(employeeCode: string): Observable<EmployeeDetail> {
    return this.http.post<EmployeeDetail>(`${this.baseUrl}/${employeeCode}/archive`, null);
  }

  reactivate(employeeCode: string): Observable<EmployeeDetail> {
    return this.http.post<EmployeeDetail>(`${this.baseUrl}/${employeeCode}/reactivate`, null);
  }
}
