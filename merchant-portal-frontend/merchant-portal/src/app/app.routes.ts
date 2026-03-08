import { Routes } from '@angular/router';
import { MerchantRegisterComponent } from './merchant/register-merchants/register-merchants.component';
import { MerchantStatusComponent } from './merchant/check-merchant-status/check-merchant-status.component';
import { DashboardComponent } from './officer/view-dashboard/view-dashboard.component';
import { AdminRegisterComponent } from './officer/register-admins/register-admins.component';
import { ManageAdminsComponent } from './officer/manage-admins/manage-admins.component';
import { ViewApplicationComponent } from './officer/view-applications/view-applications.component';
import { LoginComponent } from './officer/login/login.component';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/merchant/register', pathMatch: 'full' },

  // Merchant routes (Public)
  { path: 'merchant/register', component: MerchantRegisterComponent },
  { path: 'merchant/status', component: MerchantStatusComponent },

  // Officer routes (Protected)
  { path: 'officer/login', component: LoginComponent }, // <-- 3. Add the new login route
  { 
    path: 'officer/dashboard', 
    component: DashboardComponent,
    canActivate: [authGuard] // <-- 4. Protect this route
  },
  { 
    path: 'officer/register-admins', 
    component: AdminRegisterComponent,
    canActivate: [authGuard, roleGuard] // <-- 4. Protect this route
  },
  { 
    path: 'officer/manage-admins', 
    component: ManageAdminsComponent,
    canActivate: [authGuard, roleGuard]
  },
  { 
    path: 'officer/view/:id', 
    component: ViewApplicationComponent,
    canActivate: [authGuard] // <-- 4. Protect this route
  },

  { path: '**', redirectTo: '/merchant/register' } // Default fallback
];