import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MerchantService } from './merchant.service';

describe('MerchantService — Unit Tests', () => {
  let service: MerchantService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MerchantService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(MerchantService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  describe('getAllMerchants', () => {
    it('should GET /api/merchants', () => {
      service.getAllMerchants().subscribe(res => {
        expect(res.length).toBe(1);
      });
      const req = httpMock.expectOne('http://localhost:8001/api/merchants');
      expect(req.request.method).toBe('GET');
      req.flush([{ merchantId: 1, merchantName: 'Test' }]);
    });
  });

  describe('getMerchantById', () => {
    it('should GET /api/merchants/:id', () => {
      service.getMerchantById(1).subscribe(res => {
        expect(res.merchantName).toBe('Test');
      });
      const req = httpMock.expectOne('http://localhost:8001/api/merchants/1');
      req.flush({ merchantId: 1, merchantName: 'Test' });
    });
  });

  describe('createMerchant', () => {
    it('should POST /api/merchants', () => {
      const merchant = { merchantName: 'New', status: 'ACTIVE' };
      service.createMerchant(merchant).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/merchants');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(merchant);
      req.flush({ merchantId: 2, ...merchant });
    });
  });

  describe('updateMerchant', () => {
    it('should PUT /api/merchants/:id', () => {
      service.updateMerchant(1, { merchantName: 'Updated' }).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/merchants/1');
      expect(req.request.method).toBe('PUT');
      req.flush({});
    });
  });

  describe('deleteMerchant', () => {
    it('should DELETE /api/merchants/:id', () => {
      service.deleteMerchant(1).subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/merchants/1');
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });

  describe('searchMerchants', () => {
    it('should GET /api/merchants/search with name param', () => {
      service.searchMerchants('Test').subscribe(res => {
        expect(res.length).toBe(1);
      });
      const req = httpMock.expectOne('http://localhost:8001/api/merchants/search?name=Test');
      req.flush([{ merchantId: 1, merchantName: 'Test Merchant' }]);
    });
  });

  describe('getMerchantUsers', () => {
    it('should GET /api/merchants/:id/users', () => {
      service.getMerchantUsers(1).subscribe(res => {
        expect(res.length).toBe(2);
      });
      const req = httpMock.expectOne('http://localhost:8001/api/merchants/1/users');
      req.flush([{ userId: 1 }, { userId: 2 }]);
    });
  });

  describe('assignUserToMerchant', () => {
    it('should POST /api/merchants/:id/users/:userId', () => {
      service.assignUserToMerchant(1, '5').subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/merchants/1/users/5');
      expect(req.request.method).toBe('POST');
      req.flush({});
    });
  });

  describe('removeUserFromMerchant', () => {
    it('should DELETE /api/merchants/:id/users/:userId', () => {
      service.removeUserFromMerchant(1, '5').subscribe();
      const req = httpMock.expectOne('http://localhost:8001/api/merchants/1/users/5');
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });
});
