import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreditAdviceDetailComponent } from './credit-advice-detail.component';

describe('CreditAdviceDetailComponent', () => {
  let component: CreditAdviceDetailComponent;
  let fixture: ComponentFixture<CreditAdviceDetailComponent>;

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [ CreditAdviceDetailComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CreditAdviceDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
