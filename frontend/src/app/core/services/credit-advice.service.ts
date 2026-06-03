import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class CreditAdviceService {
  private apiUrl = 'http://localhost:8001/api/credit-advices';

  constructor(private http: HttpClient) {}

  getAllCreditAdvices(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  getCreditAdvicesPage(params: {
    page: number; size: number; sortBy?: string; sortDir?: string;
    merchantName?: string; accountId?: string;
    dateFrom?: string; dateTo?: string;
  }): Observable<any> {
    let httpParams = new HttpParams()
      .set('page', params.page)
      .set('size', params.size)
      .set('sortBy', params.sortBy || 'paymentDate')
      .set('sortDir', params.sortDir || 'desc');
    if (params.merchantName) httpParams = httpParams.set('merchantName', params.merchantName);
    if (params.accountId) httpParams = httpParams.set('accountId', params.accountId);
    if (params.dateFrom) httpParams = httpParams.set('dateFrom', params.dateFrom);
    if (params.dateTo) httpParams = httpParams.set('dateTo', params.dateTo);
    return this.http.get<any>(this.apiUrl, { params: httpParams });
  }

  getCreditAdviceById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  searchCreditAdvices(keyword: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/search?keyword=${keyword}`);
  }
}
