import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RefundService } from '../../../core/services/refund.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { MaskCardPipe } from '../../../shared/pipes/mask-card.pipe';

@Component({
  selector: 'app-refund-list',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent, MaskCardPipe],
  templateUrl: './refund-list.component.html',
  styleUrls: ['./refund-list.component.css']
})
export class RefundListComponent implements OnInit {
  refunds: any[] = [];
  totalItems = 0;
  totalPages = 0;

  filterMerchant = '';
  filterRefundRef = '';
  filterTransactionId = '';
  filterStatus = '';
  filterType = '';
  dateFrom = '';
  dateTo = '';
  loading = true;
  loadError = false;

  currentPage = 1;
  pageSize = 10;
  sortField = 'submissionDate';
  sortDirection: 'asc' | 'desc' = 'desc';

  private filterTimer: any;

  constructor(
    private router: Router,
    private refundService: RefundService,
  ) {}

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage() {
    this.loading = true;
    this.loadError = false;
    this.refundService.getRefundsPage({
      page: this.currentPage - 1,
      size: this.pageSize,
      sortBy: this.sortField,
      sortDir: this.sortDirection,
      merchantName: this.filterMerchant || undefined,
      refundRefNo: this.filterRefundRef || undefined,
      transactionId: this.filterTransactionId || undefined,
      status: this.filterStatus || undefined,
      refundType: this.filterType || undefined,
      dateFrom: this.dateFrom || undefined,
      dateTo: this.dateTo || undefined,
    }).subscribe({
      next: (data) => {
        this.refunds = data.content ?? [];
        this.totalItems = data.totalElements ?? 0;
        this.totalPages = data.totalPages ?? 0;
        this.loading = false;
      },
      error: () => { this.loading = false; this.loadError = true; }
    });
  }

  onFilterChange() {
    clearTimeout(this.filterTimer);
    this.filterTimer = setTimeout(() => {
      this.currentPage = 1;
      this.loadPage();
    }, 400);
  }

  onPageChange(page: number) { this.currentPage = page; this.loadPage(); }
  onPageSizeChange(size: number) { this.pageSize = size; this.currentPage = 1; this.loadPage(); }

  sortBy(field: string) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'desc';
    }
    this.currentPage = 1;
    this.loadPage();
  }

  getSortIcon(field: string): string {
    if (this.sortField !== field) return '\u21C5';
    return this.sortDirection === 'asc' ? '\u2191' : '\u2193';
  }

  clearFilters() {
    this.filterMerchant = '';
    this.filterRefundRef = '';
    this.filterTransactionId = '';
    this.filterStatus = '';
    this.filterType = '';
    this.dateFrom = '';
    this.dateTo = '';
    this.currentPage = 1;
    this.loadPage();
  }

  viewDetails(id: number) {
    this.router.navigate(['/refunds', id]);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-MY', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
