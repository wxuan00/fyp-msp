import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private apiUrl = 'http://localhost:8001/api/reports';

  private readonly BLUE:  [number,number,number] = [30, 64, 175];
  private readonly LIGHT: [number,number,number] = [239, 246, 255];

  constructor(private http: HttpClient) {}

  // ── shared helpers ────────────────────────────────────────────────
  downloadBlob(blob: Blob, filename: string) {
    const url = URL.createObjectURL(blob);
    const a   = document.createElement('a');
    a.href = url; a.download = filename; a.click();
    URL.revokeObjectURL(url);
  }

  maskCard(cardNo: string): string {
    if (!cardNo) return '-';
    const d = cardNo.replace(/\s/g, '');
    return d.length < 4 ? cardNo : '**** **** **** ' + d.slice(-4);
  }

  private buildHeader(doc: jsPDF, title: string) {
    const w     = doc.internal.pageSize.getWidth();
    const today = new Date().toLocaleDateString('en-MY', { year:'numeric', month:'long', day:'numeric' });
    doc.setFillColor(...this.BLUE);
    doc.rect(0, 0, w, 18, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(13); doc.setFont('helvetica', 'bold');
    doc.text(title, 10, 12);
    doc.setFontSize(9); doc.setFont('helvetica', 'normal');
    doc.text(today, w - 10, 12, { align: 'right' });
    doc.setTextColor(0, 0, 0);
  }

  // ── client-side PDF generators ──────────────────────────────────────
  generateTransactionsPdf(rows: any[], filename = 'transactions.pdf') {
    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    this.buildHeader(doc, 'MSP — Transaction Report');
    autoTable(doc, {
      startY: 24,
      head: [['Txn ID', 'Merchant', 'Card (masked)', 'Amount (MYR)', 'Nett Amt', 'Channel', 'Status', 'Date']],
      body: rows.map(r => [
        r.transactionId ?? r.id ?? '-',
        r.merchantName  ?? '-',
        this.maskCard(r.cardNo),
        r.amount       != null ? Number(r.amount).toFixed(2)     : '-',
        r.nettAmount   != null ? Number(r.nettAmount).toFixed(2) : '-',
        r.paymentChannel ?? r.channel ?? '-',
        r.status ?? '-',
        r.txnDate ? String(r.txnDate).slice(0, 10) : '-',
      ]),
      headStyles: { fillColor: this.BLUE, textColor: [255,255,255], fontStyle:'bold', fontSize:8 },
      alternateRowStyles: { fillColor: this.LIGHT },
      bodyStyles: { fontSize: 7.5 },
      margin: { left:10, right:10 },
      theme: 'grid',
    });
    doc.save(filename);
  }

  generateRefundsPdf(rows: any[], filename = 'refunds.pdf') {
    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    this.buildHeader(doc, 'MSP — Refund Report');
    autoTable(doc, {
      startY: 24,
      head: [['Refund ID', 'Ref No', 'Merchant', 'Txn ID', 'Amount (MYR)', 'Type', 'Status', 'Date']],
      body: rows.map(r => [
        r.refundId   ?? r.id ?? '-',
        r.refundRefNo ?? '-',
        r.merchantName ?? '-',
        r.transactionId ?? '-',
        r.amount != null ? Number(r.amount).toFixed(2) : '-',
        r.refundType ?? r.type ?? '-',
        r.status ?? '-',
        r.submissionDate ? String(r.submissionDate).slice(0, 10) : '-',
      ]),
      headStyles: { fillColor: this.BLUE, textColor: [255,255,255], fontStyle:'bold', fontSize:8 },
      alternateRowStyles: { fillColor: this.LIGHT },
      bodyStyles: { fontSize: 7.5 },
      margin: { left:10, right:10 },
      theme: 'grid',
    });
    doc.save(filename);
  }

  generateSettlementsPdf(rows: any[], filename = 'settlements.pdf') {
    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    this.buildHeader(doc, 'MSP — Settlement Report');
    autoTable(doc, {
      startY: 24,
      head: [['Sett. ID', 'Settlement No', 'Type', 'Credit Advice ID', 'Currency', 'Sett. Amount', 'Payment Amt', 'Date']],
      body: rows.map(r => [
        r.settlementId  ?? r.id ?? '-',
        r.settlementNo  ?? '-',
        r.settlementType ?? '-',
        r.creditAdviceId ?? '-',
        r.currency      ?? 'MYR',
        r.settlementAmount != null ? Number(r.settlementAmount).toFixed(2) : '-',
        r.paymentAmount    != null ? Number(r.paymentAmount).toFixed(2)    : '-',
        r.settlementDate ? String(r.settlementDate).slice(0, 10) : '-',
      ]),
      headStyles: { fillColor: this.BLUE, textColor: [255,255,255], fontStyle:'bold', fontSize:8 },
      alternateRowStyles: { fillColor: this.LIGHT },
      bodyStyles: { fontSize: 7.5 },
      margin: { left:10, right:10 },
      theme: 'grid',
    });
    doc.save(filename);
  }

  generateCreditAdvicesPdf(rows: any[], filename = 'credit-advices.pdf') {
    const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });
    this.buildHeader(doc, 'MSP — Credit Advice Report');
    autoTable(doc, {
      startY: 24,
      head: [['CA ID', 'Merchant', 'Account No', 'Amount (MYR)', 'Currency', 'Status', 'Payment Date']],
      body: rows.map(r => [
        r.creditAdviceId ?? r.id ?? '-',
        r.merchantName   ?? '-',
        r.accountNo      ?? '-',
        r.amount != null ? Number(r.amount).toFixed(2) : '-',
        r.currency       ?? 'MYR',
        r.status         ?? '-',
        r.paymentDate ? String(r.paymentDate).slice(0, 10) : '-',
      ]),
      headStyles: { fillColor: this.BLUE, textColor: [255,255,255], fontStyle:'bold', fontSize:8 },
      alternateRowStyles: { fillColor: this.LIGHT },
      bodyStyles: { fontSize: 7.5 },
      margin: { left:10, right:10 },
      theme: 'grid',
    });
    doc.save(filename);
  }

  getSummaryReport(startDate?: string, endDate?: string): Observable<any> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<any>(`${this.apiUrl}/summary`, { params });
  }

  exportSummaryReportCsv(startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get(`${this.apiUrl}/summary/export`, {
      params,
      responseType: 'blob'
    });
  }

  exportTransactionsCsv(startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get(`${this.apiUrl}/transactions/export`, {
      params,
      responseType: 'blob'
    });
  }

  exportSettlementsCsv(startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get(`${this.apiUrl}/settlements/export`, {
      params,
      responseType: 'blob'
    });
  }

  exportSummaryReportPdf(startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get(`${this.apiUrl}/summary/export/pdf`, { params, responseType: 'blob' });
  }

  exportTransactionsPdf(startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get(`${this.apiUrl}/transactions/export/pdf`, { params, responseType: 'blob' });
  }

  exportSettlementsPdf(startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get(`${this.apiUrl}/settlements/export/pdf`, { params, responseType: 'blob' });
  }

  // ── single-record detail PDF generators ────────────────────────────────────

  generateTransactionDetailPdf(t: any) {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    this.buildHeader(doc, 'MSP \u2014 Transaction Receipt');
    const rows: [string, string][] = [
      ['Transaction ID',   '#' + (t.transactionId ?? '-')],
      ['Merchant',         t.merchantName        ?? '-'],
      ['Payment Channel',  t.paymentChannel      ?? '-'],
      ['Status',           t.status              ?? '-'],
      ['Amount',           (t.currency ?? 'MYR') + ' ' + (t.amount        != null ? Number(t.amount).toFixed(2)        : '-')],
      ['Discount',         (t.currency ?? 'MYR') + ' ' + (t.discountAmount != null ? Number(t.discountAmount).toFixed(2) : '-')],
      ['Nett Amount',      (t.currency ?? 'MYR') + ' ' + (t.nettAmount    != null ? Number(t.nettAmount).toFixed(2)    : '-')],
      ['Card No',          this.maskCard(t.cardNo)],
      ['Ref No',           t.refNo            ?? '-'],
      ['Description',      t.txnDescription   ?? '-'],
      ['Transaction Date', t.txnDate    ? String(t.txnDate).slice(0, 19).replace('T', ' ')    : '-'],
      ['Posted Date',      t.postedDate ? String(t.postedDate).slice(0, 19).replace('T', ' ') : '-'],
    ];
    autoTable(doc, {
      startY: 24, head: [['Field', 'Value']], body: rows,
      headStyles: { fillColor: this.BLUE, textColor: [255,255,255], fontStyle: 'bold', fontSize: 9 },
      alternateRowStyles: { fillColor: this.LIGHT }, bodyStyles: { fontSize: 9 },
      columnStyles: { 0: { fontStyle: 'bold', cellWidth: 55 } },
      margin: { left: 20, right: 20 }, theme: 'grid',
    });
    doc.save(`transaction-${t.transactionId ?? 'detail'}.pdf`);
  }

  generateRefundDetailPdf(r: any) {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    this.buildHeader(doc, 'MSP \u2014 Refund Detail');
    const rows: [string, string][] = [
      ['Refund ID',        '#' + (r.refundId      ?? '-')],
      ['Transaction ID',   '#' + (r.transactionId ?? '-')],
      ['Merchant',         r.merchantName    ?? '-'],
      ['Status',           r.status === 'APPROVED' ? 'Refunded' : (r.status ?? '-')],
      ['Refund Type',      r.refundType      ?? '-'],
      ['Original Amount',  (r.currency ?? 'MYR') + ' ' + (r.amount       != null ? Number(r.amount).toFixed(2)       : '-')],
      ['Refund Amount',    (r.currency ?? 'MYR') + ' ' + (r.refundAmount  != null ? Number(r.refundAmount).toFixed(2)  : '-')],
      ['Refund Ref No',    r.refundRefNo     ?? '-'],
      ['Card No',          this.maskCard(r.cardNo)],
      ['Transaction Date', r.transactionDate ? String(r.transactionDate).slice(0, 19).replace('T', ' ') : '-'],
      ['Submission Date',  r.submissionDate  ? String(r.submissionDate).slice(0, 19).replace('T', ' ')  : '-'],
      ['Posted Date',      r.postedDate      ? String(r.postedDate).slice(0, 19).replace('T', ' ')      : '-'],
    ];
    autoTable(doc, {
      startY: 24, head: [['Field', 'Value']], body: rows,
      headStyles: { fillColor: this.BLUE, textColor: [255,255,255], fontStyle: 'bold', fontSize: 9 },
      alternateRowStyles: { fillColor: this.LIGHT }, bodyStyles: { fontSize: 9 },
      columnStyles: { 0: { fontStyle: 'bold', cellWidth: 55 } },
      margin: { left: 20, right: 20 }, theme: 'grid',
    });
    doc.save(`refund-${r.refundId ?? 'detail'}.pdf`);
  }

  generateCreditAdviceDetailPdf(ca: any, settlements: any[]) {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    this.buildHeader(doc, 'MSP \u2014 Credit Advice Detail');
    const rows: [string, string][] = [
      ['Credit Advice ID', '#' + (ca.creditAdviceId ?? '-')],
      ['Merchant',         ca.merchantName  ?? '-'],
      ['Amount',           (ca.currency ?? 'MYR') + ' ' + (ca.amount != null ? Number(ca.amount).toFixed(2) : '-')],
      ['Account No',       ca.accountNo     ?? '-'],
      ['Account ID',       ca.accountId     ?? '-'],
      ['Payment Date',     ca.paymentDate ? String(ca.paymentDate).slice(0, 10) : '-'],
    ];
    autoTable(doc, {
      startY: 24, head: [['Field', 'Value']], body: rows,
      headStyles: { fillColor: this.BLUE, textColor: [255,255,255], fontStyle: 'bold', fontSize: 9 },
      alternateRowStyles: { fillColor: this.LIGHT }, bodyStyles: { fontSize: 9 },
      columnStyles: { 0: { fontStyle: 'bold', cellWidth: 55 } },
      margin: { left: 20, right: 20 }, theme: 'grid',
    });
    if (settlements.length > 0) {
      const y = (doc as any).lastAutoTable.finalY + 10;
      doc.setFontSize(11); doc.setFont('helvetica', 'bold');
      doc.text('Linked Settlements (' + settlements.length + ')', 20, y);
      autoTable(doc, {
        startY: y + 4,
        head: [['Sett. ID', 'Settlement No', 'Type', 'Sett. Amount', 'Payment Amt', 'Date']],
        body: settlements.map(s => [
          '#' + (s.settlementId ?? '-'),
          s.settlementNo ?? '-',
          s.settlementType ?? '-',
          (s.currency ?? 'MYR') + ' ' + (s.settlementAmount != null ? Number(s.settlementAmount).toFixed(2) : '-'),
          (s.currency ?? 'MYR') + ' ' + (s.paymentAmount    != null ? Number(s.paymentAmount).toFixed(2)    : '-'),
          s.settlementDate ? String(s.settlementDate).slice(0, 10) : '-',
        ]),
        headStyles: { fillColor: this.BLUE, textColor: [255,255,255], fontStyle: 'bold', fontSize: 8 },
        alternateRowStyles: { fillColor: this.LIGHT }, bodyStyles: { fontSize: 8 },
        margin: { left: 20, right: 20 }, theme: 'grid',
      });
    }
    doc.save(`credit-advice-${ca.creditAdviceId ?? 'detail'}.pdf`);
  }

  generateSettlementDetailPdf(s: any, transactions: any[]) {
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    this.buildHeader(doc, 'MSP \u2014 Settlement Detail');
    const rows: [string, string][] = [
      ['Settlement ID',     '#' + (s.settlementId ?? '-')],
      ['Settlement No',     s.settlementNo           ?? '-'],
      ['Merchant',          s.merchantName           ?? '-'],
      ['Settlement Type',   s.settlementType         ?? '-'],
      ['Settlement Amount', (s.currency ?? 'MYR') + ' ' + (s.settlementAmount != null ? Number(s.settlementAmount).toFixed(2) : '-')],
      ['Payment Amount',    (s.currency ?? 'MYR') + ' ' + (s.paymentAmount    != null ? Number(s.paymentAmount).toFixed(2)    : '-')],
      ['Credit Advice ID',  s.creditAdviceId ?? '-'],
      ['Settlement Date',   s.settlementDate ? String(s.settlementDate).slice(0, 10) : '-'],
    ];
    autoTable(doc, {
      startY: 24, head: [['Field', 'Value']], body: rows,
      headStyles: { fillColor: this.BLUE, textColor: [255,255,255], fontStyle: 'bold', fontSize: 9 },
      alternateRowStyles: { fillColor: this.LIGHT }, bodyStyles: { fontSize: 9 },
      columnStyles: { 0: { fontStyle: 'bold', cellWidth: 55 } },
      margin: { left: 20, right: 20 }, theme: 'grid',
    });
    if (transactions.length > 0) {
      const y = (doc as any).lastAutoTable.finalY + 10;
      doc.setFontSize(11); doc.setFont('helvetica', 'bold');
      doc.text('Linked Transactions (' + transactions.length + ')', 20, y);
      autoTable(doc, {
        startY: y + 4,
        head: [['Txn ID', 'Merchant', 'Card (masked)', 'Channel', 'Amount', 'Status', 'Date']],
        body: transactions.map(t => [
          '#' + (t.transactionId ?? '-'),
          t.merchantName   ?? '-',
          this.maskCard(t.cardNo),
          t.paymentChannel ?? '-',
          (t.currency ?? 'MYR') + ' ' + (t.amount != null ? Number(t.amount).toFixed(2) : '-'),
          t.status ?? '-',
          t.txnDate ? String(t.txnDate).slice(0, 10) : '-',
        ]),
        headStyles: { fillColor: this.BLUE, textColor: [255,255,255], fontStyle: 'bold', fontSize: 8 },
        alternateRowStyles: { fillColor: this.LIGHT }, bodyStyles: { fontSize: 8 },
        margin: { left: 20, right: 20 }, theme: 'grid',
      });
    }
    doc.save(`settlement-${s.settlementId ?? 'detail'}.pdf`);
  }
}
