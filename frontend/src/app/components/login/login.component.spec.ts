import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { LoginComponent } from './login.component';
import { AuthService } from '../../auth.service';
import { ToastService } from '../../toast.service';

describe('LoginComponent', () => {
  let authService: { login: ReturnType<typeof vi.fn>; currentRole: ReturnType<typeof vi.fn> };
  let toastService: { show: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authService = { login: vi.fn(), currentRole: vi.fn().mockReturnValue('TENANT_ADMIN') };
    toastService = { show: vi.fn() };
    router = { navigate: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: ToastService, useValue: toastService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();
  });

  it('rejects invalid inputs before calling auth service', () => {
    const component = TestBed.createComponent(LoginComponent).componentInstance;
    component.username = '';
    component.password = '';
    component.tenantId = '';

    component.submit();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('successful tenant login navigates to dashboard', () => {
    authService.login.mockReturnValue(of({ token: 'abc', isTemporaryPassword: false }));
    const component = TestBed.createComponent(LoginComponent).componentInstance;
    component.username = 'admin_bank';
    component.password = 'ValidPass@123';
    component.tenantId = 'BANK_A';

    component.submit();
    expect(authService.login).toHaveBeenCalledWith('admin_bank', 'ValidPass@123', 'BANK_A', false);
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard'], { replaceUrl: true });
  });

  it('login with temporary password navigates to reset-password', () => {
    authService.login.mockReturnValue(of({ token: 'abc', isTemporaryPassword: true }));
    const component = TestBed.createComponent(LoginComponent).componentInstance;
    component.username = 'new_officer';
    component.password = 'TempPass@123';
    component.tenantId = 'BANK_A';

    component.submit();
    expect(router.navigate).toHaveBeenCalledWith(['/reset-password'], { replaceUrl: true });
  });

  it('system login navigates to master when role is SYSTEM_ADMIN', () => {
    authService.currentRole.mockReturnValue('SYSTEM_ADMIN');
    authService.login.mockReturnValue(of({ token: 'master-token', isTemporaryPassword: false }));
    const component = TestBed.createComponent(LoginComponent).componentInstance;
    component.master = true;
    component.username = 'superadmin';
    component.password = 'MasterSecret!123';

    component.submit();
    expect(authService.login).toHaveBeenCalledWith('superadmin', 'MasterSecret!123', '', true);
    expect(router.navigate).toHaveBeenCalledWith(['/master'], { replaceUrl: true });
  });

  it('handles login error by showing error toast', () => {
    authService.login.mockReturnValue(throwError(() => ({ error: { message: 'Invalid credentials' } })));
    const component = TestBed.createComponent(LoginComponent).componentInstance;
    component.username = 'admin_bank';
    component.password = 'WrongPass';
    component.tenantId = 'BANK_A';

    component.submit();
    expect(toastService.show).toHaveBeenCalledWith('Invalid credentials', 'danger');
    expect(component.loading).toBe(false);
  });
});
