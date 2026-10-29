import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from './auth.service';
import { sessionInterceptor } from './session.interceptor';

describe('HR session', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [
      provideRouter([]), provideHttpClient(withInterceptors([sessionInterceptor])), provideHttpClientTesting(),
    ] });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('renews the CSRF cookie before completing login and logout', async () => {
    const login = firstValueFrom(auth.login('hr.test', 'synthetic-password'));
    http.expectOne('/api/auth/login').flush({ username: 'hr.test', role: 'HR' });
    http.expectOne('/api/auth/csrf').flush(null);
    await login;
    expect(auth.currentUser()?.username).toBe('hr.test');
    const logout = firstValueFrom(auth.logout());
    http.expectOne('/api/auth/logout').flush(null);
    http.expectOne('/api/auth/csrf').flush(null);
    await logout;
    expect(auth.currentUser()).toBeNull();
  });

  it('clears expired sessions and redirects to login without retrying a write', async () => {
    const login = firstValueFrom(auth.login('hr.test', 'synthetic-password'));
    http.expectOne('/api/auth/login').flush({ username: 'hr.test', role: 'HR' });
    http.expectOne('/api/auth/csrf').flush(null);
    await login;
    const navigation = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const write = firstValueFrom(TestBed.inject(HttpClient).post('/api/employees', {}));
    const failure = expect(write).rejects.toMatchObject({ status: 401 });
    http.expectOne('/api/employees').flush({}, { status: 401, statusText: 'Unauthorized' });
    await failure;
    expect(auth.currentUser()).toBeNull();
    expect(navigation).toHaveBeenCalledWith(['/login']);
    http.expectNone('/api/employees');
  });
});
