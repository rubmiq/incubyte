import { Country, Department, EmployeeStatus } from './employee-reference-data';

export interface EmployeeSummary {
  readonly employeeCode: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string;
  readonly country: Country;
  readonly department: Department;
  readonly jobTitle: string;
  readonly status: EmployeeStatus;
  readonly hiredOn: string;
}

export interface EmployeeDetail extends EmployeeSummary {
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface EmployeeFormValue {
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string;
  readonly country: Country;
  readonly department: Department;
  readonly jobTitle: string;
  readonly hiredOn: string;
}

export interface PageResponse<T> {
  readonly content: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

export interface EmployeeListQuery {
  readonly search?: string;
  readonly country?: Country;
  readonly department?: Department;
  readonly status?: EmployeeStatus;
  readonly page?: number;
  readonly size?: number;
  readonly sort?: string;
}
