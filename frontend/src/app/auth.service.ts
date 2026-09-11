import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { map, tap } from 'rxjs';
import { ApiResponse, LoginResponse } from './models';

const TOKEN_KEY = 'aml_token';
const ROLE_KEY = 'aml_role';
const TENANT_KEY = 'aml_tenant';
const USER_KEY = 'aml_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly currentRole = signal<string | null>(sessionStorage.getItem(ROLE_KEY));
  readonly tenantId = signal<string | null>(sessionStorage.getItem(TENANT_KEY));

  constructor(private http: HttpClient, private router: Router) {}

  login(username: string, password: string, tenantId: string, master = false) {
    const body = master ? { username, password } : { username, password, tenantId };
    const endpoint = master ? '/api/v1/auth/master/login' : '/api/v1/auth/login';
    return this.http.post<ApiResponse<LoginResponse> | LoginResponse>(endpoint, body).pipe(
      map((response) => ('data' in response && response.data ? response.data : response) as LoginResponse),
      tap((result) => {
        sessionStorage.setItem(TOKEN_KEY, result.token);
        const role = this.normalizeRole(this.readRole(result.token) || (master ? 'SYSTEM_ADMIN' : 'TENANT_ADMIN'));
        sessionStorage.setItem(ROLE_KEY, role);
        const serverTenant = master ? 'MASTER' : this.readTenant(result.token) || tenantId;
        sessionStorage.setItem(TENANT_KEY, serverTenant.toUpperCase());
        sessionStorage.setItem(USER_KEY, username);
        this.currentRole.set(role);
        this.tenantId.set(serverTenant.toUpperCase());
      })
    );
  }

  resetPassword(payload: object) {
    return this.http.post('/api/v1/auth/reset-password', payload);
  }

  token() { return sessionStorage.getItem(TOKEN_KEY); }
  username() { return sessionStorage.getItem(USER_KEY) || ''; }
  isAuthenticated() { return !!this.token(); }
  logout() { sessionStorage.clear(); this.currentRole.set(null); this.tenantId.set(null); this.router.navigate(['/login'], { replaceUrl: true }); }

  private readRole(token: string) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const authority = payload.authorities?.[0];
      return payload.role || payload.roles?.[0] || (typeof authority === 'string' ? authority : authority?.authority) || null;
    } catch { return null; }
  }

  private readTenant(token: string) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.tenantId || payload.tenant_id || payload.tenant || null;
    } catch { return null; }
  }

  private normalizeRole(role: unknown) {
    const value = String(role || '').toUpperCase().replace(/^ROLE_/, '');
    if (value === 'BANK_ADMIN') return 'TENANT_ADMIN';
    return ['SYSTEM_ADMIN', 'TENANT_ADMIN', 'COMPLIANCE_OFFICER'].includes(value) ? value : 'TENANT_ADMIN';
  }
}
