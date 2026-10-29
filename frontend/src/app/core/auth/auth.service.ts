import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';

import { CurrentUser } from './current-user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly currentUserState = signal<CurrentUser | null>(null);
  private readonly initializedState = signal(false);

  readonly currentUser = this.currentUserState.asReadonly();
  readonly initialized = this.initializedState.asReadonly();

  clearSession(): void {
    this.currentUserState.set(null);
  }

  /** Seeds the CSRF cookie and resolves the active session, run once at app bootstrap. */
  initialize(): Observable<void> {
    return this.refreshCsrfCookie().pipe(
      switchMap(() => this.http.get<CurrentUser>('/api/auth/me')),
      tap((user) => this.currentUserState.set(user)),
      catchError((error: HttpErrorResponse) => {
        if (error.status !== 401) {
          throw error;
        }
        this.currentUserState.set(null);
        return of(null);
      }),
      tap(() => this.initializedState.set(true)),
      map(() => void 0),
    );
  }

  login(username: string, password: string): Observable<CurrentUser> {
    return this.http.post<CurrentUser>('/api/auth/login', { username, password }).pipe(
      tap((user) => this.currentUserState.set(user)),
      switchMap((user) => this.refreshCsrfCookie().pipe(map(() => user))),
    );
  }

  logout(): Observable<void> {
    return this.http.post('/api/auth/logout', null, { observe: 'response' }).pipe(
      tap(() => this.currentUserState.set(null)),
      switchMap(() => this.refreshCsrfCookie()),
    );
  }

  /**
   * The XSRF-TOKEN cookie is cleared by the server on login/logout, so it must be
   * re-issued afterwards (and once at startup) before the next state-changing request.
   */
  private refreshCsrfCookie(): Observable<void> {
    return this.http.get<void>('/api/auth/csrf').pipe(map(() => void 0));
  }
}
