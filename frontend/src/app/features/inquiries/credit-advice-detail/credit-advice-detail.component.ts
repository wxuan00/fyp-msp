import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { CreditAdviceService } from '../../../core/services/credit-advice.service';
import { ReportService } from '../../../core/services/report.service';
import { SettlementService } from '../../../core/services/settlement.service';

@Component({
  selector: 'app-credit-advice-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './credit-advice-detail.component.html',
  styleUrls: ['./credit-advice-detail.component.css']
})
export class CreditAdviceDetailComponent implements OnInit {
  creditAdvice: any = null;
  creditAdviceId: number = 0;
  loading = true;
  settlements: any[] = [];
  settlementsLoading = false;

  constructor(
    private route: ActivatedRoute,
    private creditAdviceService: CreditAdviceService,
    private reportService: ReportService,
    private settlementService: SettlementService
  ) {}

  ngOnInit(): void {
    this.creditAdviceId = +this.route.snapshot.paramMap.get('id')!;
    this.creditAdviceService.getCreditAdviceById(this.creditAdviceId).subscribe({
      next: (data) => {
        this.creditAdvice = data;
        this.loading = false;
        this.loadSettlements();
      },
      error: () => { this.loading = false; }
    });
  }

  loadSettlements(): void {
    this.settlementsLoading = true;
    this.settlementService.getSettlementsByCreditAdvice(this.creditAdviceId).subscribe({
      next: (data) => { this.settlements = data; this.settlementsLoading = false; },
      error: () => { this.settlementsLoading = false; }
    });
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-MY', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatAmount(val: any): string {
    if (val == null) return '-';
    return Number(val).toLocaleString('en-MY', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  exportDetailPdf(): void {
    if (this.creditAdvice) this.reportService.generateCreditAdviceDetailPdf(this.creditAdvice, this.settlements);
  }
}
