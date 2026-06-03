import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RoleService } from './role.service';

describe('RoleService — Unit Tests', () => {
  let service: RoleService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RoleService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(RoleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('getAllRoles', () => {
    it('should GET /api/roles', () => {
      service.getAllRoles().subscribe(res => {
        expect(res.length).toBe(2);
      });
      const req = httpMock.expectOne('http://localhost:8001/api/roles');
      expect(req.request.method).toBe('GET');
      req.flush([{ roleId: 1, roleName: 'ADMIN' }, { roleId: 2, roleName: 'MERCHANT' }]);
    });
  });

  describe('getRoleById', () => {
    it('should GET /api/roles/:id', () => {
      service.getRoleById(1).subscribe(res => {
        expect(res.roleName).toBe('ADMIN');
      });
      const req = httpMock.expectOne('http://localhost:8001/api/roles/1');
      req.flush({ roleId: 1, roleName: 'ADMIN' });
    });
  });

  describe('createRole', () => {
    it('should POST /api/roles', () => {
      const role = { roleName: 'TESTER', description: 'Test', roleType: 'CUSTOM' };
      service.createRole(role).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/roles');
      expect(req.request.method).toBe('POST');
      req.flush({ roleId: 3, ...role });
    });
  });

  describe('updateRole', () => {
    it('should PUT /api/roles/:id', () => {
      service.updateRole(1, { roleName: 'Updated' }).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/roles/1');
      expect(req.request.method).toBe('PUT');
      req.flush({});
    });
  });

  describe('deleteRole', () => {
    it('should DELETE /api/roles/:id', () => {
      service.deleteRole(1).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/roles/1');
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });

  describe('getAllPermissions', () => {
    it('should GET /api/roles/permissions/all', () => {
      service.getAllPermissions().subscribe(res => {
        expect(res.length).toBe(3);
      });
      const req = httpMock.expectOne('http://localhost:8001/api/roles/permissions/all');
      req.flush([
        { permissionId: 1, permissionName: 'VIEW_USERS' },
        { permissionId: 2, permissionName: 'MANAGE_USERS' },
        { permissionId: 3, permissionName: 'MANAGE_ROLES' }
      ]);
    });
  });

  describe('getRolePermissions', () => {
    it('should GET /api/roles/:id/permissions', () => {
      service.getRolePermissions(1).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/roles/1/permissions');
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  describe('updateRolePermissions', () => {
    it('should PUT /api/roles/:id/permissions', () => {
      service.updateRolePermissions(1, [1, 2, 3]).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/roles/1/permissions');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual([1, 2, 3]);
      req.flush({});
    });
  });
});
