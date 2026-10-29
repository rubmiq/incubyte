import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../auth/auth.service';

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

  protected readonly sidenavOpen = signal(true);
  protected readonly currentUser = this.authService.currentUser;

  protected readonly navItems: NavItem[] = [
    { label: 'Employees', icon: 'people', route: '/employees' },
    { label: 'Dashboard', icon: 'analytics', route: '/dashboard' },
  ];

  protected logout(): void {
    this.authService.logout();
  }

  protected toggleSidenav(): void {
    this.sidenavOpen.update((v) => !v);
  }
}
