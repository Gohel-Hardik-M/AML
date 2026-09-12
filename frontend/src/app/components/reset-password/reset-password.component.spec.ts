import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { ResetPasswordComponent } from './reset-password.component';
import { AuthService } from '../../auth.service';
import { ToastService } from '../../toast.service';

describe('ResetPasswordComponent', () => {
  let authService: {
    resetPassword: ReturnType<typeof vi.fn>;
    currentRole: ReturnType<typeof vi.fn>;
  };
  let toastService: { show: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authService = {
      resetPassword: vi.fn(),
      currentRole: vi.fn().mockReturnValue('TENANT_ADMIN')
    };
    toastService = { show: vi.fn() };
    router = { navigate: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ResetPasswordComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: ToastService, useValue: toastService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();
  });

  it('rejects submission when formRef is invalid', () => {
    const fixture = TestBed.createComponent(ResetPasswordComponent);
    const comp = fixture.componentInstance;

    comp.currentPassword = 'OldPass@123';
    comp.newPassword = 'NewPass@12345';
    comp.confirmation = 'NewPass@12345';

    comp.submit({ invalid: true });
    expect(authService.resetPassword).not.toHaveBeenCalled();
  });

  it('rejects submission when confirmation does not match new password', () => {
    const fixture = TestBed.createComponent(ResetPasswordComponent);
    const comp = fixture.componentInstance;

    comp.currentPassword = 'OldPass@123';
    comp.newPassword = 'NewPass@12345';
    comp.confirmation = 'DifferentPass@12345';

    comp.submit({ invalid: false });
    expect(authService.resetPassword).not.toHaveBeenCalled();
  });

  it('submits valid reset password and routes to dashboard for tenant users', () => {
    authService.resetPassword.mockReturnValue(of({ message: 'Success' }));
    authService.currentRole.mockReturnValue('TENANT_ADMIN');

    const fixture = TestBed.createComponent(ResetPasswordComponent);
    const comp = fixture.componentInstance;

    comp.currentPassword = 'OldPass@123';
    comp.newPassword = 'NewPass@12345';
    comp.confirmation = 'NewPass@12345';

    comp.submit({ invalid: false });
    expect(authService.resetPassword).toHaveBeenCalledWith({
      currentPassword: 'OldPass@123',
      newPassword: 'NewPass@12345'
    });
    expect(toastService.show).toHaveBeenCalledWith('Password updated successfully.');
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard'], { replaceUrl: true });
  });

  it('submits valid reset password and routes to /master for SYSTEM_ADMIN', () => {
    authService.resetPassword.mockReturnValue(of({ message: 'Success' }));
    authService.currentRole.mockReturnValue('SYSTEM_ADMIN');

    const fixture = TestBed.createComponent(ResetPasswordComponent);
    const comp = fixture.componentInstance;

    comp.currentPassword = 'AdminPass@123';
    comp.newPassword = 'SuperSecret@12345';
    comp.confirmation = 'SuperSecret@12345';

    comp.submit({ invalid: false });
    expect(router.navigate).toHaveBeenCalledWith(['/master'], { replaceUrl: true });
  });

  it('displays error toast when reset password API fails', () => {
    authService.resetPassword.mockReturnValue(throwError(() => ({
      error: { message: 'Current password is incorrect.' }
    })));

    const fixture = TestBed.createComponent(ResetPasswordComponent);
    const comp = fixture.componentInstance;

    comp.currentPassword = 'WrongOldPassword';
    comp.newPassword = 'NewPass@12345';
    comp.confirmation = 'NewPass@12345';

    comp.submit({ invalid: false });
    expect(toastService.show).toHaveBeenCalledWith('Current password is incorrect.', 'danger');
    expect(router.navigate).not.toHaveBeenCalled();
  });
});
