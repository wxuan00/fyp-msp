import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SettlementService } from './settlement.service';

describe('SettlementService — Unit Tests', () => {
  let service: SettlementService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SettlementService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(SettlementService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('getSettlementsPage (paginated)', () => {
    it('should GET /api/settlements with params', () => {
      service.getSettlementsPage({page: 0, size: 10, sortBy: 'settlementDate', sortDir: 'desc'}).subscribe((res: any) => {
        expect(res.content.length).toBe(1);
      });
      const req = httpMock.expectOne(r => r.url.includes('/api/settlements'));
      expect(req.request.method).toBe('GET');
      req.flush({ content: [{ settlementId: 1 }], totalElements: 1 });
    });
  });

  describe('getSettlementById', () => {
    it('should GET /api/settlements/:id', () => {
      service.getSettlementById(1).subscribe(res => {
        expect(res.settlementId).toBe(1);
      });
      const req = httpMock.expectOne('http://localhost:8001/api/settlements/1');
      req.flush({ settlementId: 1, settlementNo: 'STL001' });
    });
  });
});
