import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RefundService } from './refund.service';

describe('RefundService — Unit Tests', () => {
  let service: RefundService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        RefundService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(RefundService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('getRefundsPage (paginated)', () => {
    it('should GET /api/refunds with params', () => {
      service.getRefundsPage({ page: 0, size: 10 }).subscribe((res: any) => {
        expect(res.content.length).toBe(1);
      });
      const req = httpMock.expectOne(r => r.url.includes('/api/refunds'));
      expect(req.request.method).toBe('GET');
      req.flush({ content: [{ refundId: 1 }], totalElements: 1 });
    });
  });

  describe('getRefundById', () => {
    it('should GET /api/refunds/:id', () => {
      service.getRefundById(1).subscribe((res: any) => {
        expect(res.refundId).toBe(1);
      });
      const req = httpMock.expectOne('http://localhost:8001/api/refunds/1');
      req.flush({ refundId: 1, refundRefNo: 'RFD001' });
    });
  });

  describe('requestRefund', () => {
    it('should POST /api/refunds', () => {
      const refund = { transactionId: 100, merchantId: 10, amount: 200 };
      service.requestRefund(refund).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/refunds');
      expect(req.request.method).toBe('POST');
      req.flush({ refundId: 1 });
    });
  });

  describe('cancelRefund', () => {
    it('should PUT /api/refunds/:id/cancel', () => {
      service.cancelRefund(1).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/refunds/1/cancel');
      expect(req.request.method).toBe('PUT');
      req.flush({});
    });
  });
});
