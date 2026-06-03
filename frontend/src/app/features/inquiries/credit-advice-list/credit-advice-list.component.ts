import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CreditAdviceService } from '../../../core/services/credit-advice.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';

@Component({
  selector: 'app-credit-advice-list',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './credit-advice-list.component.html',
  styleUrls: ['./credit-advice-list.component.css']
})
export class CreditAdviceListComponent implements OnInit {
  creditAdvices: any[] = [];
  totalItems = 0;
  totalPages = 0;

  filterMerchant = '';
  filterAccountId = '';
  filterStatus = '';
  dateFrom = '';
  dateTo = '';
  loading = true;
  loadError = false;

  currentPage = 1;
  pageSize = 10;
  sortField = 'paymentDate';
  sortDirection: 'asc' | 'desc' = 'desc';

  private filterTimer: any;

  constructor(
    private router: Router,
    private creditAdviceService: CreditAdviceService,
  ) {}

  ngOnInit(): void {
    this.loadPage();
  }

  loadPage() {
    this.loading = true;
    this.loadError = false;
    this.creditAdviceService.getCreditAdvicesPage({
      page: this.currentPage - 1,
      size: this.pageSize,
      sortBy: this.sortField,
      sortDir: this.sortDirection,
      merchantName: this.filterMerchant || undefined,
      accountId: this.filterAccountId || undefined,
      dateFrom: this.dateFrom || undefined,
      dateTo: this.dateTo || undefined,
    }).subscribe({
      next: (data) => {
        this.creditAdvices = data.content ?? [];
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
    this.filterAccountId = '';
    this.filterStatus = '';
    this.dateFrom = '';
    this.dateTo = '';
    this.currentPage = 1;
    this.loadPage();
  }

  viewDetails(id: number) {
    this.router.navigate(['/credit-advices', id]);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-MY', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
