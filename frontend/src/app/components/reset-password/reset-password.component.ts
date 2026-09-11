import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth.service';
import { ToastService } from '../../toast.service';

@Component({ selector: 'app-reset-password', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './reset-password.component.html', styleUrl: './reset-password.component.css' })
export class ResetPasswordComponent {
  currentPassword = ''; newPassword = ''; confirmation = ''; showCurrent = false; showNew = false; showConfirmation = false;
  private auth = inject(AuthService); private router = inject(Router); private toast = inject(ToastService);
  submit(formRef: any) { if (formRef.invalid || this.confirmation !== this.newPassword) return; this.auth.resetPassword({ tenantId: this.auth.tenantId(), username: this.auth.username(), currentPassword: this.currentPassword, newPassword: this.newPassword }).subscribe({ next: () => { this.toast.show('Password updated successfully.'); this.router.navigate([this.auth.currentRole() === 'SYSTEM_ADMIN' ? '/master' : '/dashboard'], { replaceUrl: true }); }, error: error => this.toast.show(error.error?.message || 'Unable to update password.', 'danger') }); }
}
