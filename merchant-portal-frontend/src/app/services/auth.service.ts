import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api/auth';
  private authTokenKey = 'auth-token';
  private authRoleKey = 'auth-role';
  private authPermissionsKey = 'auth-permissions';

  constructor(private http: HttpClient, private router: Router) { }

  login(credentials: { username: string, password: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      tap((response: any) => {
        // Only save token/role if TOTP is not required
        if (!response.requireTotp) {
          this.saveToken(response.token);
          this.saveRole(response.role);
        }
      })
    );
  }

  verifyTotp(payload: { username: string, code: string | number }): Observable<any> {
    // Convert code to integer if it's a string
    const body = {
      username: payload.username,
      code: typeof payload.code === 'string' ? parseInt(payload.code, 10) : payload.code
    };
    console.log('Sending verifyTotp payload:', body);
    return this.http.post(`${this.apiUrl}/verify-totp`, body);
  }

  /** Fetches permissions from backend and stores them in localStorage */
  fetchAndStorePermissions(): Observable<any> {
    return this.http.get(`${this.apiUrl}/my-permissions`).pipe(
      tap((res: any) => {
        if (res.permissions) {
          this.savePermissions(res.permissions);
        }
      })
    );
  }

  setupTotp(username?: string): Observable<any> {
    const body = username ? { username } : {};
    return this.http.post(`${this.apiUrl}/setup-totp`, body);
  }

  saveTempToken(token: string): void {
    sessionStorage.setItem('temp-token', token);
  }

  getTempToken(): string | null {
    return sessionStorage.getItem('temp-token');
  }

  clearTempToken(): void {
    sessionStorage.removeItem('temp-token');
  }

  saveToken(token: string): void {
    localStorage.setItem(this.authTokenKey, token);
  }

  getToken(): string | null {
    return localStorage.getItem(this.authTokenKey);
  }

  saveRole(role: string): void {
    localStorage.setItem(this.authRoleKey, role);
  }

  getRole(): string | null {
    return localStorage.getItem(this.authRoleKey);
  }

  savePermissions(permissions: string[]): void {
    localStorage.setItem(this.authPermissionsKey, JSON.stringify(permissions));
  }

  getPermissions(): string[] {
    const stored = localStorage.getItem(this.authPermissionsKey);
    return stored ? JSON.parse(stored) : [];
  }

  hasPermission(permission: string): boolean {
    return this.getPermissions().includes(permission);
  }

  // Check if user is a Reviewer (Super Admin)
  isReviewer(): boolean {
    const role = this.getRole()?.toLowerCase();
    return role === 'reviewer';
  }

  isAuthenticated(): boolean {
    // Check if a token exists
    return !!this.getToken();
  }

  logout(): void {
    localStorage.removeItem(this.authTokenKey);
    localStorage.removeItem(this.authRoleKey);
    localStorage.removeItem(this.authPermissionsKey);
    this.clearTempToken();
    this.router.navigate(['/officer/login']);
  }
}
