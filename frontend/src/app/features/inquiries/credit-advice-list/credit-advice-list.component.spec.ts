import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreditAdviceListComponent } from './credit-advice-list.component';

describe('CreditAdviceListComponent', () => {
  let component: CreditAdviceListComponent;
  let fixture: ComponentFixture<CreditAdviceListComponent>;

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [ CreditAdviceListComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CreditAdviceListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
