import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private apiUrl = 'http://localhost:8001/api/auth'; // Connects to Spring Boot

  constructor(private http: HttpClient) { }

  // 1. Login Method - now handles MFA flow
  login(credentials: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      tap((response: any) => {
        // 2. Save the Token and Role if login works
        if (response && response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('role', response.role);
          
          // Check if MFA is required
          if (response.mfaRequired) {
            sessionStorage.setItem('mfa_pending', 'true');
          }
        }
      })
    );
  }

  // MFA verification
  verifyMfa(code: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/mfa/verify`, { code });
  }

  // Check if MFA verification is pending
  isMfaPending(): boolean {
    return sessionStorage.getItem('mfa_pending') === 'true';
  }

  // Clear MFA pending status
  clearMfaPending(): void {
    sessionStorage.removeItem('mfa_pending');
  }

  // 3. Helper to get the token later
  getToken(): string | null {
    return localStorage.getItem('token');
  }

  // Get current user info from backend — also caches permissions in localStorage
  getCurrentUser(): Observable<any> {
    return this.http.get(`${this.apiUrl}/me`).pipe(
      tap((user: any) => {
        if (user?.permissions) {
          this.storePermissions(user.permissions);
        }
      })
    );
  }

  getAllUsers(): Observable<any> {
    return this.http.get('http://localhost:8001/api/users');
  }

  // Create User
  createUser(user: any): Observable<any> {
    return this.http.post('http://localhost:8001/api/users', user);
  }

  // Delete User
  deleteUser(id: number): Observable<any> {
    return this.http.delete(`http://localhost:8001/api/users/${id}`);
  }

  // Update User
  updateUser(id: number, user: any): Observable<any> {
    return this.http.put(`http://localhost:8001/api/users/${id}`, user);
  }

  // Get User by ID
  getUserById(id: number): Observable<any> {
    return this.http.get(`http://localhost:8001/api/users/${id}`);
  }

  // Get User with role + permissions details
  getUserDetails(id: number): Observable<any> {
    return this.http.get(`http://localhost:8001/api/users/${id}/details`);
  }

  checkDisplayName(name: string, excludeId?: string): Observable<any> {
    const params: any = { name };
    if (excludeId) params.excludeId = excludeId;
    return this.http.get('http://localhost:8001/api/users/check-display-name', { params });
  }

  // Admin: reset a user's password
  adminResetPassword(userId: number, newPassword: string): Observable<any> {
    return this.http.patch(`http://localhost:8001/api/users/${userId}/password`, { newPassword });
  }

  // Profile endpoints
  getProfile(): Observable<any> {
    return this.http.get('http://localhost:8001/api/profile');
  }

  updateProfile(updates: any): Observable<any> {
    return this.http.put('http://localhost:8001/api/profile', updates);
  }

  changePassword(passwords: any): Observable<any> {
    return this.http.put('http://localhost:8001/api/profile/password', passwords);
  }

  clearMustChangePassword(): Observable<any> {
    return this.http.patch('http://localhost:8001/api/auth/clear-must-change-password', {});
  }

  // ============ MFA Setup Methods ============

  // Get MFA status
  getMfaStatus(): Observable<any> {
    return this.http.get('http://localhost:8001/api/profile/mfa/status');
  }

  // Generate MFA setup (secret + QR code)
  setupMfa(): Observable<any> {
    return this.http.post('http://localhost:8001/api/profile/mfa/setup', {});
  }

  // Enable MFA with secret and verification code
  enableMfa(secret: string, code: string): Observable<any> {
    return this.http.post('http://localhost:8001/api/profile/mfa/enable', { secret, code });
  }

  // Disable MFA
  disableMfa(password: string, code: string): Observable<any> {
    return this.http.post('http://localhost:8001/api/profile/mfa/disable', { password, code });
  }

  // Forgot password - request reset token
  forgotPassword(email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/forgot-password`, { email });
  }

  // Reset password using token
  resetPassword(token: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/reset-password`, { token, newPassword });
  }

  // Helper method to get user role from localStorage
  getUserRole(): string {
    if (typeof localStorage !== 'undefined') {
      return localStorage.getItem('role') || ''; 
    }
    return '';
  }

  // Helper to check if user is Admin
  isAdmin(): boolean {
    return this.getUserRole() === 'ADMIN';
  }

  // Store permissions in localStorage
  storePermissions(permissions: string[]): void {
    localStorage.setItem('permissions', JSON.stringify(permissions));
  }

  // Get permissions from localStorage
  getPermissions(): string[] {
    try {
      return JSON.parse(localStorage.getItem('permissions') || '[]');
    } catch {
      return [];
    }
  }

  // Check if user has a specific permission (checks actual permissions from backend)
  hasPermission(permission: string): boolean {
    return this.getPermissions().includes(permission);
  }

  // Get current user's email from JWT token
  getCurrentUserEmail(): string {
    const token = this.getToken();
    if (!token) return '';
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.sub || '';
    } catch {
      return '';
    }
  }

  // Logout - clear all stored data
  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('permissions');
    sessionStorage.removeItem('mfa_pending');
  }
}