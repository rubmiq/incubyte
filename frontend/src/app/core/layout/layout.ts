import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../auth/auth.service';
import { NotificationService } from '../notification.service';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatToolbarModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
  ],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly sidenavOpen = signal(true);
  protected readonly loggingOut = signal(false);
  protected readonly currentUser = this.authService.currentUser;

  protected readonly navItems: NavItem[] = [
    { label: 'Employees', icon: 'people', route: '/employees' },
    { label: 'Dashboard', icon: 'analytics', route: '/dashboard' },
  ];

  protected logout(): void {
    if (this.loggingOut()) return;
    this.loggingOut.set(true);
    this.authService.logout().pipe(
      finalize(() => this.loggingOut.set(false)),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: () => {
        this.notifications.success('Signed out.');
        void this.router.navigate(['/login']);
      },
      error: () => {
        if (!this.authService.currentUser()) {
          // The server already ended the session; a CSRF refresh failure must
          // not leave private content visible as though sign-out failed.
          this.notifications.error('You are signed out. Reload the sign-in page before signing in again.');
          void this.router.navigate(['/login']);
          return;
        }
        this.notifications.error('Could not sign out. Please try again.');
      },
    });
  }

  protected toggleSidenav(): void {
    this.sidenavOpen.update((v) => !v);
  }
}
