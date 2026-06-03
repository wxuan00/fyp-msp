import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = 'http://localhost:8001/api/dashboard';

  constructor(private http: HttpClient) {}

  getStats(merchantId?: number, startDate?: string, endDate?: string): Observable<any> {
    let params = new HttpParams();
    if (merchantId != null) params = params.set('merchantId', merchantId.toString());
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<any>(`${this.apiUrl}/stats`, { params });
  }

  getInsights(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/insights`);
  }

  getChartData(merchantId?: number, startDate?: string, endDate?: string): Observable<any> {
    let params = new HttpParams();
    if (merchantId != null) params = params.set('merchantId', merchantId.toString());
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<any>(`${this.apiUrl}/chart-data`, { params });
  }
}
