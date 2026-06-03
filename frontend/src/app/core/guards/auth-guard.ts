import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1. Check if the token exists
  if (!authService.getToken()) {
    // No token? Kick them out to login
    router.navigate(['/login']);
    return false;
  }

  // 2. Check if MFA verification is pending
  if (authService.isMfaPending()) {
    // MFA pending - redirect to MFA page
    router.navigate(['/mfa']);
    return false;
  }

  // Token exists and MFA completed - allow access
  return true;
};