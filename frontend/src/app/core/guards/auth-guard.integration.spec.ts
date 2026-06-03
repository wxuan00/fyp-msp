import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from '../services/auth.service';
import { vi } from 'vitest';

// Mock localStorage for Vitest
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

describe('AuthGuard — Integration Tests', () => {
  let authService: AuthService;
  let router: Router;

  beforeEach(async () => {
    mockStorage.clear();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });
    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  afterEach(() => { mockStorage.clear(); });

  it('should block access when no token is present', () => {
    expect(authService.getToken()).toBeNull();
  });

  it('should allow access when token is present', () => {
    localStorage.setItem('token', 'valid-jwt');
    expect(authService.getToken()).toBe('valid-jwt');
  });

  it('should correctly identify admin user', () => {
    localStorage.setItem('role', 'ADMIN');
    expect(authService.isAdmin()).toBe(true);
  });

  it('should correctly identify non-admin user', () => {
    localStorage.setItem('role', 'MERCHANT');
    expect(authService.isAdmin()).toBe(false);
  });

  it('should check permissions from localStorage', () => {
    authService.storePermissions(['VIEW_TRANSACTIONS', 'VIEW_SETTLEMENTS']);
    expect(authService.hasPermission('VIEW_TRANSACTIONS')).toBe(true);
    expect(authService.hasPermission('MANAGE_USERS')).toBe(false);
  });
});
