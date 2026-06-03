import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { RefundService } from '../../../core/services/refund.service';
import { ReportService } from '../../../core/services/report.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { MaskCardPipe } from '../../../shared/pipes/mask-card.pipe';

@Component({
  selector: 'app-refund-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ConfirmDialogComponent, MaskCardPipe],
  templateUrl: './refund-detail.component.html',
  styleUrls: ['./refund-detail.component.css']
})
export class RefundDetailComponent implements OnInit {
  refund: any = null;
  refundId: number = 0;
  loading = true;
  showCancelDialog = false;
  cancelling = false;

  constructor(
    private route: ActivatedRoute,
    private refundService: RefundService,
    private reportService: ReportService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.refundId = +this.route.snapshot.paramMap.get('id')!;
    this.loadRefund();
  }

  loadRefund(): void {
    this.refundService.getRefundById(this.refundId).subscribe({
      next: (data) => {
        this.refund = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
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
    if (this.refund) this.reportService.generateRefundDetailPdf(this.refund);
  }

  openCancelDialog(): void {
    this.showCancelDialog = true;
  }

  dismissCancelDialog(): void {
    this.showCancelDialog = false;
  }

  confirmCancelRefund(): void {
    this.showCancelDialog = false;
    this.cancelling = true;
    this.refundService.cancelRefund(this.refundId).subscribe({
      next: () => {
        // Reload full refund with all join fields (merchantName etc.)
        this.loadRefund();
        this.cancelling = false;
        this.toastService.success('Refund request cancelled successfully.');
      },
      error: () => {
        this.cancelling = false;
        this.toastService.error('Failed to cancel refund request.');
      }
    });
  }
}
