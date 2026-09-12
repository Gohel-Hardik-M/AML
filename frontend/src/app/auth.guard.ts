import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const isResetPasswordRoute = state ? state.url.startsWith('/reset-password') : false;
  if (auth.mustChangePassword() && !isResetPasswordRoute) {
    return router.createUrlTree(['/reset-password']);
  }
  if (!auth.mustChangePassword() && isResetPasswordRoute) {
    const defaultRoute = auth.currentRole() === 'SYSTEM_ADMIN' ? '/master' : '/dashboard';
    return router.createUrlTree([defaultRoute]);
  }

  return true;
};
