import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SettlementService {
  private apiUrl = 'http://localhost:8001/api/settlements';

  constructor(private http: HttpClient) {}

  getAllSettlements(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  getSettlementsPage(params: {
    page: number; size: number; sortBy?: string; sortDir?: string;
    merchantName?: string; settlementNo?: string; settlementType?: string;
    dateFrom?: string; dateTo?: string;
  }): Observable<any> {
    let httpParams = new HttpParams()
      .set('page', params.page)
      .set('size', params.size)
      .set('sortBy', params.sortBy || 'settlementDate')
      .set('sortDir', params.sortDir || 'desc');
    if (params.merchantName) httpParams = httpParams.set('merchantName', params.merchantName);
    if (params.settlementNo) httpParams = httpParams.set('settlementNo', params.settlementNo);
    if (params.settlementType) httpParams = httpParams.set('settlementType', params.settlementType);
    if (params.dateFrom) httpParams = httpParams.set('dateFrom', params.dateFrom);
    if (params.dateTo) httpParams = httpParams.set('dateTo', params.dateTo);
    return this.http.get<any>(this.apiUrl, { params: httpParams });
  }

  getSettlementById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  getSettlementsByCreditAdvice(creditAdviceId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/by-credit-advice/${creditAdviceId}`);
  }

  searchSettlements(keyword: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/search?keyword=${keyword}`);
  }
}
