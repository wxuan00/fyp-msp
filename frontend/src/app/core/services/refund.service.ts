import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RefundService {
  private apiUrl = 'http://localhost:8001/api/refunds';

  constructor(private http: HttpClient) {}

  getAllRefunds(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  getRefundsPage(params: {
    page: number; size: number; sortBy?: string; sortDir?: string;
    merchantName?: string; refundRefNo?: string; transactionId?: string; cardNo?: string;
    status?: string; refundType?: string; dateFrom?: string; dateTo?: string;
  }): Observable<any> {
    let httpParams = new HttpParams()
      .set('page', params.page)
      .set('size', params.size)
      .set('sortBy', params.sortBy || 'submissionDate')
      .set('sortDir', params.sortDir || 'desc');
    if (params.merchantName) httpParams = httpParams.set('merchantName', params.merchantName);
    if (params.refundRefNo) httpParams = httpParams.set('refundRefNo', params.refundRefNo);
    if (params.transactionId) httpParams = httpParams.set('transactionId', params.transactionId);
    if (params.cardNo) httpParams = httpParams.set('cardNo', params.cardNo);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.refundType) httpParams = httpParams.set('refundType', params.refundType);
    if (params.dateFrom) httpParams = httpParams.set('dateFrom', params.dateFrom);
    if (params.dateTo) httpParams = httpParams.set('dateTo', params.dateTo);
    return this.http.get<any>(this.apiUrl, { params: httpParams });
  }

  getRefundById(id: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${id}`);
  }

  searchRefunds(keyword: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/search?keyword=${keyword}`);
  }

  requestRefund(refund: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, refund);
  }

  cancelRefund(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/cancel`, {});
  }
}
