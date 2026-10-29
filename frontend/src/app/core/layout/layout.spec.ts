import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatMenuHarness } from '@angular/material/menu/testing';
import { MatSnackBarHarness } from '@angular/material/snack-bar/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { Layout } from './layout';

describe('Account logout', () => {
  let fixture: ComponentFixture<Layout>;
  let http: HttpTestingController;
  let auth: AuthService;
  let navigation: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    TestBed.configureTestingModule({ imports: [Layout],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    const login = firstValueFrom(auth.login('hr.test', 'test-only-password'));
    http.expectOne('/api/auth/login').flush({ username: 'hr.test', role: 'HR' });
    http.expectOne('/api/auth/csrf').flush(null);
    await login;
    navigation = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(Layout);
    fixture.detectChanges();
  });
  afterEach(() => http.verify());

  async function clickLogout(): Promise<MatMenuHarness> {
    const menu = await TestbedHarnessEnvironment.loader(fixture)
      .getHarness(MatMenuHarness.with({ selector: '[aria-label="Account menu"]' }));
    await menu.open();
    await menu.clickItem({ text: /Logout$/ });
    return menu;
  }
  async function toast(): Promise<string> {
    const snack = await TestbedHarnessEnvironment.documentRootLoader(fixture).getHarness(MatSnackBarHarness);
    return snack.getMessage();
  }

  it('posts logout, renews CSRF, clears the session and returns to login', async () => {
    await clickLogout();
    const request = http.expectOne('/api/auth/logout');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeNull();
    request.flush(null, { status: 204, statusText: 'No Content' });
    expect(auth.currentUser()).toBeNull();
    expect(navigation).not.toHaveBeenCalled();
    http.expectOne('/api/auth/csrf').flush(null);
    fixture.detectChanges();
    expect(navigation).toHaveBeenCalledWith(['/login']);
    expect(await toast()).toBe('Success: Signed out.');
  });

  it('disables repeated logout clicks while the request is pending', async () => {
    const menu = await clickLogout();
    const request = http.expectOne('/api/auth/logout');
    await menu.open();
    const pending = await menu.getItems({ text: /Signing out…$/ });
    expect(await pending[0].isDisabled()).toBe(true);
    http.expectNone('/api/auth/logout');
    request.flush(null);
    http.expectOne('/api/auth/csrf').flush(null);
  });

  it('keeps the session on a failed logout and permits retry', async () => {
    await clickLogout();
    http.expectOne('/api/auth/logout').flush({}, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(auth.currentUser()?.username).toBe('hr.test');
    expect(navigation).not.toHaveBeenCalled();
    expect(await toast()).toBe('Error: Could not sign out. Please try again.');
    await clickLogout();
    http.expectOne('/api/auth/logout').flush(null);
    http.expectOne('/api/auth/csrf').flush(null);
    expect(navigation).toHaveBeenCalledWith(['/login']);
  });

  it('returns to login if the session ended but refreshing CSRF failed', async () => {
    await clickLogout();
    http.expectOne('/api/auth/logout').flush(null);
    http.expectOne('/api/auth/csrf').flush({}, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(auth.currentUser()).toBeNull();
    expect(navigation).toHaveBeenCalledWith(['/login']);
    expect(await toast()).toContain('You are signed out. Reload the sign-in page');
  });
});
