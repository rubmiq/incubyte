import { inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(`Success: ${message}`, 'Dismiss', {
      horizontalPosition: 'right', verticalPosition: 'top',
      duration: 5000, panelClass: 'toast-success', politeness: 'polite',
    });
  }

  error(message: string): void {
    this.snackBar.open(`Error: ${message}`, 'Dismiss', {
      horizontalPosition: 'right', verticalPosition: 'top',
      duration: 8000, panelClass: 'toast-error', politeness: 'assertive',
    });
  }
}
