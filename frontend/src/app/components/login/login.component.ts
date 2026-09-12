import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../auth.service';
import { ToastService } from '../../toast.service';
import { isTenantId } from '../../validation';

@Component({ selector: 'app-login', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './login.component.html' })
export class LoginComponent {
  username = ''; password = ''; tenantId = ''; master = false; loading = false; showPassword = false;
  private auth = inject(AuthService); private router = inject(Router); private toast = inject(ToastService);
  submit() { this.username = this.username.trim(); this.tenantId = this.tenantId.trim(); if (!this.username || this.username.length > 128 || !this.password || this.password.length > 128 || (!this.master && !isTenantId(this.tenantId))) return; this.loading = true; this.auth.login(this.username, this.password, this.tenantId, this.master).subscribe({ next: (result) => { this.loading = false; this.router.navigate([result.isTemporaryPassword ? '/reset-password' : this.auth.currentRole() === 'SYSTEM_ADMIN' ? '/master' : '/dashboard'], { replaceUrl: true }); }, error: (error) => { this.loading = false; this.toast.show(error.error?.message || 'Unable to sign in. Check your credentials.', 'danger'); } }); }
}
