import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (!auth.isAuthenticated()) return true;

  const role = auth.currentRole();
  return inject(Router).createUrlTree([role === 'SYSTEM_ADMIN' ? '/master' : '/dashboard']);
};
