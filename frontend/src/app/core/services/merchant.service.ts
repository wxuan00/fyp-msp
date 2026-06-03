import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class MerchantService {
  private apiUrl = 'http://localhost:8001/api/merchants';
  private usersUrl = 'http://localhost:8001/api/users';

  constructor(private http: HttpClient) {}

  // Get all merchants
  getAllMerchants(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  // Get merchant by ID
  getMerchantById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  // Search by name
  searchMerchants(name: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/search?name=${name}`);
  }

  // Create new merchant
  createMerchant(merchant: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, merchant);
  }

  // Update merchant
  updateMerchant(id: number, merchant: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, merchant);
  }

  // Delete merchant
  deleteMerchant(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  // ─ Merchant ↔ User Mappings ─
  getMerchantUsers(merchantId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${merchantId}/users`);
  }

  assignUserToMerchant(merchantId: number, userId: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${merchantId}/users/${userId}`, {});
  }

  removeUserFromMerchant(merchantId: number, userId: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${merchantId}/users/${userId}`);
  }

  // Search users by name/email
  searchUsers(q: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.usersUrl}/search`, { params: new HttpParams().set('q', q) });
  }

  // Get all merchants the current logged-in user is linked to
  getMyMerchants(): Observable<any[]> {
    return this.http.get<any[]>(`${this.usersUrl}/my-merchants`);
  }

  // Sync user's linked merchants (replace all with provided list)
  syncUserMerchants(userId: number, merchantIds: number[]): Observable<any> {
    return this.http.put<any>(`${this.usersUrl}/${userId}/merchants`, merchantIds);
  }
}