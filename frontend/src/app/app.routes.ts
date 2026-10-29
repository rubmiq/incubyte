import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login').then((m) => m.Login),
  },
  {
    path: 'employees',
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./employees/employee-list/employee-list').then((m) => m.EmployeeList),
      },
      {
        path: 'new',
        loadComponent: () =>
          import('./employees/employee-form/employee-form').then((m) => m.EmployeeForm),
      },
      {
        path: ':code',
        loadComponent: () =>
          import('./employees/employee-detail/employee-detail').then((m) => m.EmployeeDetailPage),
      },
      {
        path: ':code/edit',
        loadComponent: () =>
          import('./employees/employee-form/employee-form').then((m) => m.EmployeeForm),
      },
    ],
  },
  { path: '', pathMatch: 'full', redirectTo: 'employees' },
  { path: '**', redirectTo: 'employees' },
];
