import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const roles = route.data['roles'] as string[] | undefined;
  const redirectTo = route.data['redirectTo'] as string || '/dashboard';
  return !roles?.length || roles.includes(auth.currentRole() || '') || inject(Router).createUrlTree([redirectTo]);
};