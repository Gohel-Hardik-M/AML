import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth.service';
import { ToastService } from '../../toast.service';

@Component({ selector: 'app-reset-password', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './reset-password.component.html' })
export class ResetPasswordComponent {
  currentPassword = '';
  newPassword = '';
  confirmation = '';
  showCurrent = false;
  showNew = false;
  showConfirmation = false;
  loading = false;
  errorMessage = '';

  private auth = inject(AuthService);
  private router = inject(Router);
  private toast = inject(ToastService);
  private changeDetector = inject(ChangeDetectorRef);

  submit(formRef: any) {
    if (this.loading || formRef.invalid || this.confirmation !== this.newPassword) return;
    this.errorMessage = '';
    this.loading = true;
    this.auth.resetPassword({ currentPassword: this.currentPassword, newPassword: this.newPassword }).subscribe({
      next: () => {
        this.loading = false;
        this.toast.show('Password updated successfully.');
        const target = this.auth.currentRole() === 'SYSTEM_ADMIN' ? '/master' : '/dashboard';
        this.router.navigate([target], { replaceUrl: true });
      },
      error: error => {
        this.loading = false;
        this.errorMessage = this.extractErrorMessage(error);
        this.toast.show(this.errorMessage, 'danger');
        this.changeDetector.detectChanges();
      }
    });
  }

  logout() {
    this.auth.logout();
  }

  private extractErrorMessage(error: any): string {
    if (!error) return 'Unable to update password. Please try again.';
    if (typeof error === 'string') {
      try {
        const parsed = JSON.parse(error);
        if (parsed?.message) return parsed.message;
      } catch {}
      return error;
    }
    if (typeof error.error === 'string') {
      try {
        const parsed = JSON.parse(error.error);
        if (parsed?.message) return parsed.message;
      } catch {}
      return error.error;
    }
    if (error.error?.message) return error.error.message;
    if (error.message) return error.message;
    return 'Unable to update password. Please check your current password.';
  }
}
