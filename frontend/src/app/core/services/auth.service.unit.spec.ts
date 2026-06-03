import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { vi } from 'vitest';

// Mock localStorage and sessionStorage for Vitest
const store: Record<string, string> = {};
const mockStorage = {
  getItem: vi.fn((key: string) => store[key] ?? null),
  setItem: vi.fn((key: string, val: string) => { store[key] = val; }),
  removeItem: vi.fn((key: string) => { delete store[key]; }),
  clear: vi.fn(() => { Object.keys(store).forEach(k => delete store[k]); }),
  get length() { return Object.keys(store).length; },
  key: vi.fn((i: number) => Object.keys(store)[i] ?? null),
};
Object.defineProperty(globalThis, 'localStorage', { value: mockStorage, writable: true });
Object.defineProperty(globalThis, 'sessionStorage', { value: mockStorage, writable: true });

describe('AuthService — Unit Tests', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    mockStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    mockStorage.clear();
  });

  describe('login', () => {
    it('should POST credentials and store token on success', () => {
      const creds = { email: 'user@test.com', password: 'pass' };
      service.login(creds).subscribe(res => {
        expect(res.token).toBe('jwt-token');
        expect(localStorage.getItem('token')).toBe('jwt-token');
        expect(localStorage.getItem('role')).toBe('ADMIN');
      });
      const req = httpMock.expectOne('http://localhost:8001/api/auth/login');
      expect(req.request.method).toBe('POST');
      req.flush({ token: 'jwt-token', role: 'ADMIN' });
    });

    it('should set mfa_pending when mfaRequired is true', () => {
      service.login({ email: 'a@b.com', password: 'p' }).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/auth/login');
      req.flush({ token: 'tok', role: 'ADMIN', mfaRequired: true });
      expect(sessionStorage.getItem('mfa_pending')).toBe('true');
    });
  });

  describe('verifyMfa', () => {
    it('should POST mfa code', () => {
      service.verifyMfa('123456').subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/auth/mfa/verify');
      expect(req.request.body).toEqual({ code: '123456' });
      req.flush({ success: true });
    });
  });

  describe('getCurrentUser', () => {
    it('should GET /api/auth/me and cache permissions', () => {
      service.getCurrentUser().subscribe(user => {
        expect(user.email).toBe('admin@msp.com');
      });
      const req = httpMock.expectOne('http://localhost:8001/api/auth/me');
      req.flush({ email: 'admin@msp.com', permissions: ['VIEW_USERS', 'MANAGE_USERS'] });
      expect(service.getPermissions()).toEqual(['VIEW_USERS', 'MANAGE_USERS']);
    });
  });

  describe('forgotPassword', () => {
    it('should POST email', () => {
      service.forgotPassword('user@test.com').subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/auth/forgot-password');
      expect(req.request.body).toEqual({ email: 'user@test.com' });
      req.flush({ message: 'ok' });
    });
  });

  describe('resetPassword', () => {
    it('should POST token and new password', () => {
      service.resetPassword('tok123', 'NewPass!').subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/auth/reset-password');
      expect(req.request.body).toEqual({ token: 'tok123', newPassword: 'NewPass!' });
      req.flush({ message: 'ok' });
    });
  });

  describe('helper methods', () => {
    it('getToken returns stored token', () => {
      localStorage.setItem('token', 'abc');
      expect(service.getToken()).toBe('abc');
    });

    it('getUserRole returns stored role', () => {
      localStorage.setItem('role', 'MERCHANT');
      expect(service.getUserRole()).toBe('MERCHANT');
    });

    it('isAdmin returns true for ADMIN role', () => {
      localStorage.setItem('role', 'ADMIN');
      expect(service.isAdmin()).toBe(true);
    });

    it('isAdmin returns false for non-ADMIN role', () => {
      localStorage.setItem('role', 'MERCHANT');
      expect(service.isAdmin()).toBe(false);
    });

    it('hasPermission checks permissions array', () => {
      service.storePermissions(['VIEW_USERS', 'MANAGE_ROLES']);
      expect(service.hasPermission('VIEW_USERS')).toBe(true);
      expect(service.hasPermission('DELETE_USERS')).toBe(false);
    });

    it('isMfaPending checks session storage', () => {
      expect(service.isMfaPending()).toBe(false);
      sessionStorage.setItem('mfa_pending', 'true');
      expect(service.isMfaPending()).toBe(true);
    });

    it('logout clears all storage', () => {
      localStorage.setItem('token', 'x');
      localStorage.setItem('role', 'y');
      localStorage.setItem('permissions', '[]');
      sessionStorage.setItem('mfa_pending', 'true');
      service.logout();
      expect(localStorage.getItem('token')).toBeNull();
      expect(localStorage.getItem('role')).toBeNull();
      expect(sessionStorage.getItem('mfa_pending')).toBeNull();
    });

    it('getCurrentUserEmail decodes JWT payload', () => {
      // Create a mock JWT with sub="test@test.com"
      const payload = btoa(JSON.stringify({ sub: 'test@test.com' }));
      const fakeJwt = `header.${payload}.sig`;
      localStorage.setItem('token', fakeJwt);
      expect(service.getCurrentUserEmail()).toBe('test@test.com');
    });
  });

  describe('CRUD operations', () => {
    it('getAllUsers GET /api/users', () => {
      service.getAllUsers().subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/users');
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('createUser POST /api/users', () => {
      service.createUser({ email: 'new@test.com' }).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/users');
      expect(req.request.method).toBe('POST');
      req.flush({ id: 1 });
    });

    it('updateUser PUT /api/users/:id', () => {
      service.updateUser(1, { displayName: 'New' }).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/users/1');
      expect(req.request.method).toBe('PUT');
      req.flush({});
    });

    it('deleteUser DELETE /api/users/:id', () => {
      service.deleteUser(1).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/users/1');
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });
});
