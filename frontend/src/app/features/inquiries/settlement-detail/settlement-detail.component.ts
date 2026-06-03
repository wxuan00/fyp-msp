import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { SettlementService } from '../../../core/services/settlement.service';
import { ReportService } from '../../../core/services/report.service';
import { TransactionService } from '../../../core/services/transaction.service';
import { MaskCardPipe } from '../../../shared/pipes/mask-card.pipe';

@Component({
  selector: 'app-settlement-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, MaskCardPipe],
  templateUrl: './settlement-detail.component.html',
  styleUrls: ['./settlement-detail.component.css']
})
export class SettlementDetailComponent implements OnInit {
  settlement: any = null;
  settlementId: number = 0;
  loading = true;
  transactions: any[] = [];
  transactionsLoading = false;

  constructor(
    private route: ActivatedRoute,
    private settlementService: SettlementService,
    private reportService: ReportService,
    private transactionService: TransactionService
  ) {}

  ngOnInit(): void {
    this.settlementId = +this.route.snapshot.paramMap.get('id')!;
    this.settlementService.getSettlementById(this.settlementId).subscribe({
      next: (data) => {
        this.settlement = data;
        this.loading = false;
        this.loadTransactions();
      },
      error: () => { this.loading = false; }
    });
  }

  loadTransactions(): void {
    this.transactionsLoading = true;
    this.transactionService.getTransactionsBySettlement(this.settlementId).subscribe({
      next: (data) => { this.transactions = data; this.transactionsLoading = false; },
      error: () => { this.transactionsLoading = false; }
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
    if (this.settlement) this.reportService.generateSettlementDetailPdf(this.settlement, this.transactions);
  }
}
