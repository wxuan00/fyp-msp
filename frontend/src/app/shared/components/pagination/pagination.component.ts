import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="pagination-bar" *ngIf="totalItems > 0">
      <div class="pagination-info">
        Showing {{ startItem }}–{{ endItem }} of {{ totalItems }} results
      </div>
      <div class="pagination-controls">
        <select class="page-size-select" [ngModel]="pageSize" (ngModelChange)="onPageSizeChange($event)">
          <option [value]="10">10 / page</option>
          <option [value]="25">25 / page</option>
          <option [value]="50">50 / page</option>
          <option [value]="100">100 / page</option>
        </select>
        <div class="page-buttons">
          <button class="page-btn" (click)="goToPage(1)" [disabled]="currentPage === 1" title="First">«</button>
          <button class="page-btn" (click)="goToPage(currentPage - 1)" [disabled]="currentPage === 1" title="Previous">‹</button>
          @for (p of visiblePages; track p) {
            <button class="page-btn" [class.active]="p === currentPage" (click)="goToPage(p)">{{ p }}</button>
          }
          <button class="page-btn" (click)="goToPage(currentPage + 1)" [disabled]="currentPage === totalPages" title="Next">›</button>
          <button class="page-btn" (click)="goToPage(totalPages)" [disabled]="currentPage === totalPages" title="Last">»</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .pagination-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px 24px;
      border-top: 1px solid #e5e5e5;
      background: #fafafa;
      font-size: 0.85rem;
    }
    .pagination-info { color: #888; }
    .pagination-controls { display: flex; align-items: center; gap: 12px; }
    .page-size-select {
      padding: 6px 10px;
      border: 1px solid #ddd;
      border-radius: 6px;
      font-size: 0.82rem;
      background: #fff;
      cursor: pointer;
    }
    .page-buttons { display: flex; gap: 4px; }
    .page-btn {
      width: 34px; height: 34px;
      border: 1px solid #ddd;
      border-radius: 6px;
      background: #fff;
      cursor: pointer;
      font-size: 0.85rem;
      font-weight: 600;
      color: #333;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.15s;
    }
    .page-btn:hover:not(:disabled):not(.active) {
      background: #f0f0f0;
      border-color: #111;
    }
    .page-btn.active {
      background: #111;
      color: #fff;
      border-color: #111;
    }
    .page-btn:disabled {
      opacity: 0.35;
      cursor: not-allowed;
    }
    @media (max-width: 640px) {
      .pagination-bar { flex-direction: column; gap: 12px; }
    }
  `]
})
export class PaginationComponent {
  @Input() currentPage = 1;
  @Input() pageSize = 10;
  @Input() totalItems = 0;

  @Output() pageChange = new EventEmitter<number>();
  @Output() pageSizeChange = new EventEmitter<number>();

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalItems / this.pageSize));
  }

  get startItem(): number {
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get endItem(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalItems);
  }

  get visiblePages(): number[] {
    const pages: number[] = [];
    const total = this.totalPages;
    const current = this.currentPage;
    let start = Math.max(1, current - 2);
    let end = Math.min(total, current + 2);

    if (end - start < 4) {
      if (start === 1) end = Math.min(total, start + 4);
      else start = Math.max(1, end - 4);
    }

    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }

  onPageSizeChange(size: number): void {
    this.pageSizeChange.emit(+size);
  }
}
