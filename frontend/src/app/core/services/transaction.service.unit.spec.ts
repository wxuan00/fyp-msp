import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TransactionService } from './transaction.service';

describe('TransactionService — Unit Tests', () => {
  let service: TransactionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TransactionService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(TransactionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('getTransactionsPage (paginated)', () => {
    it('should GET /api/transactions with pagination params', () => {
      service.getTransactionsPage({page: 0, size: 10, sortBy: 'txnDate', sortDir: 'desc'}).subscribe((res: any) => {
        expect(res.content.length).toBe(1);
      });
      const req = httpMock.expectOne(r => r.url.includes('/api/transactions'));
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      req.flush({ content: [{ transactionId: 1 }], totalElements: 1 });
    });
  });

  describe('getTransactionById', () => {
    it('should GET /api/transactions/:id', () => {
      service.getTransactionById(1).subscribe(res => {
        expect(res.transactionId).toBe(1);
      });
      const req = httpMock.expectOne('http://localhost:8001/api/transactions/1');
      req.flush({ transactionId: 1, status: 'PENDING' });
    });
  });
});
