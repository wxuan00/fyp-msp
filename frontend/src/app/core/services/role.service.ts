import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RoleService {
  private apiUrl = 'http://localhost:8001/api/roles';

  constructor(private http: HttpClient) {}

  getAllRoles(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  getAllRolesWithPermissions(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/with-permissions`);
  }

  getAllPermissions(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/permissions/all`);
  }

  getRolePermissions(roleId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${roleId}/permissions`);
  }

  updateRolePermissions(roleId: number, permissionIds: number[]): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${roleId}/permissions`, permissionIds);
  }

  getRoleById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  createRole(role: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, role);
  }

  updateRole(id: number, role: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, role);
  }

  deleteRole(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  assignUserRole(userId: number, roleId: number | null): Observable<any> {
    return this.http.put<any>(`http://localhost:8001/api/users/${userId}/role`, { roleId });
  }

  syncUserRoles(userId: number, roleIds: number[]): Observable<any> {
    return this.http.put<any>(`http://localhost:8001/api/users/${userId}/roles`, roleIds);
  }

  unassignUserRole(userId: number, roleId: number): Observable<any> {
    return this.http.delete<any>(`http://localhost:8001/api/users/${userId}/roles/${roleId}`);
  }
}
