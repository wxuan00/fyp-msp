import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TransactionService } from '../../../core/services/transaction.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { MaskCardPipe } from '../../../shared/pipes/mask-card.pipe';

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent, MaskCardPipe],
  templateUrl: './transaction-list.component.html',
  styleUrls: ['./transaction-list.component.css']
})
export class TransactionListComponent implements OnInit {
  transactions: any[] = [];
  totalItems = 0;
  totalPages = 0;

  filterTxnId = '';
  filterMerchant = '';
  filterCard = '';
  filterStatus = '';
  filterChannel = '';
  dateFrom = '';
  dateTo = '';
  loading = true;
  loadError = false;

  currentPage = 1;
  pageSize = 10;
  sortField = 'txnDate';
  sortDirection: 'asc' | 'desc' = 'desc';

  private filterTimer: any;

  constructor(
    private router: Router,
    private transactionService: TransactionService,
  ) {}

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage() {
    this.loading = true;
    this.loadError = false;
    this.transactionService.getTransactionsPage({
      page: this.currentPage - 1,
      size: this.pageSize,
      sortBy: this.sortField,
      sortDir: this.sortDirection,
      merchantName: this.filterMerchant || undefined,
      txnId: this.filterTxnId || undefined,
      cardNo: this.filterCard || undefined,
      status: this.filterStatus || undefined,
      channel: this.filterChannel || undefined,
      dateFrom: this.dateFrom || undefined,
      dateTo: this.dateTo || undefined,
    }).subscribe({
      next: (data) => {
        this.transactions = data.content ?? [];
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

  onPageChange(page: number) {
    this.currentPage = page;
    this.loadPage();
  }

  onPageSizeChange(size: number) {
    this.pageSize = size;
    this.currentPage = 1;
    this.loadPage();
  }

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
    this.filterTxnId = '';
    this.filterMerchant = '';
    this.filterCard = '';
    this.filterStatus = '';
    this.filterChannel = '';
    this.dateFrom = '';
    this.dateTo = '';
    this.currentPage = 1;
    this.loadPage();
  }

  viewDetails(id: number) {
    this.router.navigate(['/transactions', id]);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-MY', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
