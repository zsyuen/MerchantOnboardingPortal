import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true; // User is logged in, allow access
  } else {
    // User is not logged in, redirect to login page
    alert('Access Denied: You must be logged in to view this page.');
    router.navigate(['/officer/login']);
    return false;
  }
};