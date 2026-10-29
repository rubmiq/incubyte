import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { Layout } from './core/layout/layout';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./auth/login/login').then((m) => m.Login),
  },
  {
    path: '',
    component: Layout,
    canActivate: [authGuard],
    children: [
      {
        path: 'employees',
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
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./analytics/dashboard/dashboard').then((m) => m.Dashboard),
      },
      { path: '', pathMatch: 'full', redirectTo: 'employees' },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
