import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CreditAdviceService } from './credit-advice.service';

describe('CreditAdviceService — Unit Tests', () => {
  let service: CreditAdviceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CreditAdviceService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(CreditAdviceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('getCreditAdvicesPage (paginated)', () => {
    it('should GET /api/credit-advices with params', () => {
      service.getCreditAdvicesPage({page: 0, size: 10, sortBy: 'paymentDate', sortDir: 'desc'}).subscribe((res: any) => {
        expect(res.content.length).toBe(1);
      });
      const req = httpMock.expectOne(r => r.url.includes('/api/credit-advices'));
      expect(req.request.method).toBe('GET');
      req.flush({ content: [{ creditAdviceId: 1 }], totalElements: 1 });
    });
  });

  describe('getCreditAdviceById', () => {
    it('should GET /api/credit-advices/:id', () => {
      service.getCreditAdviceById(1).subscribe(res => {
        expect(res.creditAdviceId).toBe(1);
      });
      const req = httpMock.expectOne('http://localhost:8001/api/credit-advices/1');
      req.flush({ creditAdviceId: 1, accountNo: 'ACC001' });
    });
  });
});
