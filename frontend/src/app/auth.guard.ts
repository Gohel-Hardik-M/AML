import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  const url = state ? state.url : '';
  const isPasswordChangeRoute = url.startsWith('/reset-password') || url.startsWith('/change-password');

  // If user has a temporary password, force them to change password first
  if (auth.mustChangePassword() && !isPasswordChangeRoute) {
    return router.createUrlTree(['/reset-password']);
  }

  // Authenticated users are free to change their password anytime!
  return true;
};
