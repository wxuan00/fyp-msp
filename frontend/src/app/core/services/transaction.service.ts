import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private apiUrl = 'http://localhost:8001/api/transactions';

  constructor(private http: HttpClient) {}

  getAllTransactions(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  getTransactionsPage(params: {
    page: number; size: number; sortBy?: string; sortDir?: string;
    merchantName?: string; txnId?: string; cardNo?: string;
    status?: string; channel?: string; dateFrom?: string; dateTo?: string;
  }): Observable<any> {
    let httpParams = new HttpParams()
      .set('page', params.page)
      .set('size', params.size)
      .set('sortBy', params.sortBy || 'txnDate')
      .set('sortDir', params.sortDir || 'desc');
    if (params.merchantName) httpParams = httpParams.set('merchantName', params.merchantName);
    if (params.txnId) httpParams = httpParams.set('txnId', params.txnId);
    if (params.cardNo) httpParams = httpParams.set('cardNo', params.cardNo);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.channel) httpParams = httpParams.set('channel', params.channel);
    if (params.dateFrom) httpParams = httpParams.set('dateFrom', params.dateFrom);
    if (params.dateTo) httpParams = httpParams.set('dateTo', params.dateTo);
    return this.http.get<any>(this.apiUrl, { params: httpParams });
  }

  getTransactionById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  getTransactionsBySettlement(settlementId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/by-settlement/${settlementId}`);
  }

  searchTransactions(keyword: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/search?keyword=${keyword}`);
  }
}
