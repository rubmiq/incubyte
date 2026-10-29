export const COUNTRIES = ['US', 'GB', 'DE', 'FR', 'NL', 'IN', 'BR', 'AU', 'CA', 'SG'] as const;
export type Country = (typeof COUNTRIES)[number];

export const COUNTRY_LABELS: Record<Country, string> = {
  US: 'United States',
  GB: 'United Kingdom',
  DE: 'Germany',
  FR: 'France',
  NL: 'Netherlands',
  IN: 'India',
  BR: 'Brazil',
  AU: 'Australia',
  CA: 'Canada',
  SG: 'Singapore',
};

export const DEPARTMENTS = [
  'ENGINEERING',
  'PRODUCT',
  'SALES',
  'MARKETING',
  'FINANCE',
  'HUMAN_RESOURCES',
  'OPERATIONS',
  'CUSTOMER_SUPPORT',
  'LEGAL',
  'IT',
] as const;
export type Department = (typeof DEPARTMENTS)[number];

export const DEPARTMENT_LABELS: Record<Department, string> = {
  ENGINEERING: 'Engineering',
  PRODUCT: 'Product',
  SALES: 'Sales',
  MARKETING: 'Marketing',
  FINANCE: 'Finance',
  HUMAN_RESOURCES: 'Human Resources',
  OPERATIONS: 'Operations',
  CUSTOMER_SUPPORT: 'Customer Support',
  LEGAL: 'Legal',
  IT: 'IT',
};

export const EMPLOYEE_STATUSES = ['ACTIVE', 'ARCHIVED'] as const;
export type EmployeeStatus = (typeof EMPLOYEE_STATUSES)[number];
