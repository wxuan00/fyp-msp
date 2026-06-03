import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1. Check if the token exists
  if (!authService.getToken()) {
    router.navigate(['/login']);
    return false;
  }

  // 2. Check if user is ADMIN
  if (authService.isAdmin()) {
    return true;
  } else {
    // Not an admin - redirect to dashboard
    router.navigate(['/dashboard']);
    return false;
  }
};
