import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TransactionService } from '../../../core/services/transaction.service';
import { RefundService } from '../../../core/services/refund.service';
import { ReportService } from '../../../core/services/report.service';
import { ToastService } from '../../../core/services/toast.service';
import { MaskCardPipe } from '../../../shared/pipes/mask-card.pipe';

@Component({
  selector: 'app-transaction-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, MaskCardPipe],
  templateUrl: './transaction-detail.component.html',
  styleUrls: ['./transaction-detail.component.css']
})
export class TransactionDetailComponent implements OnInit {
  transaction: any = null;
  transactionId: number = 0;
  loading = true;

  // Refund modal state
  showRefundModal = false;
  refundSubmitting = false;
  refundForm = {
    refundType: 'FULL',
    refundAmount: 0,
    reason: ''
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService,
    private refundService: RefundService,
    private reportService: ReportService,
    private toast: ToastService,
  ) {}

  ngOnInit(): void {
    this.transactionId = +this.route.snapshot.paramMap.get('id')!;
    this.transactionService.getTransactionById(this.transactionId).subscribe({
      next: (data) => {
        this.transaction = data;
        this.refundForm.refundAmount = data.amount ?? 0;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  openRefundModal(): void {
    this.refundForm = {
      refundType: 'FULL',
      refundAmount: this.transaction?.amount ?? 0,
      reason: ''
    };
    this.showRefundModal = true;
  }

  onRefundTypeChange(): void {
    if (this.refundForm.refundType === 'FULL') {
      this.refundForm.refundAmount = this.transaction?.amount ?? 0;
    }
  }

  submitRefund(): void {
    if (this.refundSubmitting) return;
    this.refundSubmitting = true;

    const payload = {
      transactionId: this.transaction.transactionId,
      merchantId: this.transaction.merchantId,
      cardNo: this.transaction.cardNo,
      currency: this.transaction.currency,
      amount: this.transaction.amount,
      refundAmount: this.refundForm.refundAmount,
      refundType: this.refundForm.refundType,
      transactionDate: this.transaction.txnDate
    };

    this.refundService.requestRefund(payload).subscribe({
      next: () => {
        this.showRefundModal = false;
        this.refundSubmitting = false;
        this.toast.success('Refund request submitted successfully');
        // Reload transaction to reflect updated status
        this.transactionService.getTransactionById(this.transactionId).subscribe({
          next: (data) => this.transaction = data
        });
      },
      error: (err) => {
        this.refundSubmitting = false;
        this.toast.error(err?.error?.error || 'Failed to submit refund request');
      }
    });
  }

  formatDate(dateStr: string | null): string {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-MY', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  exportDetailPdf(): void {
    if (this.transaction) this.reportService.generateTransactionDetailPdf(this.transaction);
  }
}
