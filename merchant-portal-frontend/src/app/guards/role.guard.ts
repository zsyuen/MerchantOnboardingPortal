import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // 1. First, ensure the user is actually logged in (defensive check)
  if (!authService.isAuthenticated()) {
    alert('Access Denied: You must be logged in to access this page.');
    router.navigate(['/officer/login']);
    return false;
  }

  // 2. Reviewers (super admins) always have access; otherwise check specific permission
  if (authService.isReviewer() || authService.hasPermission('MANAGE_ADMIN_ACCESS')) {
    return true; // Authorized
  } else {
    // 3. User is logged in but does NOT have the required permission
    alert('Access Denied: You do not have permission to manage admins.');
    router.navigate(['/officer/dashboard']);
    return false;
  }
};
