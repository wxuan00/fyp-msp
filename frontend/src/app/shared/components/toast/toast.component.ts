import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ToastService, Toast } from '../../../core/services/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-container">
      @for (toast of toasts; track toast.id) {
        <div class="toast" [class]="'toast-' + toast.type" [class.toast-exit]="toast.exiting">
          <span class="toast-icon">{{ getIcon(toast.type) }}</span>
          <span class="toast-message">{{ toast.message }}</span>
          <button class="toast-close" (click)="removeToast(toast.id)">✕</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .toast-container {
      position: fixed;
      top: 68px;
      right: 24px;
      z-index: 9999;
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-width: 400px;
    }

    .toast {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 16px;
      border-radius: 10px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.12);
      animation: slideIn 0.3s ease;
      font-size: 0.84rem;
      font-weight: 500;
      backdrop-filter: blur(10px);
    }

    .toast-exit {
      animation: slideOut 0.3s ease forwards;
    }

    .toast-success {
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
      color: #065f46;
    }

    .toast-error {
      background: #fef2f2;
      border: 1px solid #fecaca;
      color: #991b1b;
    }

    .toast-warning {
      background: #fffbeb;
      border: 1px solid #fde68a;
      color: #92400e;
    }

    .toast-info {
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      color: #1e40af;
    }

    .toast-icon {
      font-size: 1.1rem;
      flex-shrink: 0;
    }

    .toast-message {
      flex: 1;
      line-height: 1.4;
    }

    .toast-close {
      background: none;
      border: none;
      cursor: pointer;
      font-size: 0.8rem;
      opacity: 0.5;
      padding: 2px;
      color: inherit;
    }

    .toast-close:hover {
      opacity: 1;
    }

    @keyframes slideIn {
      from { transform: translateX(100%); opacity: 0; }
      to { transform: translateX(0); opacity: 1; }
    }

    @keyframes slideOut {
      from { transform: translateX(0); opacity: 1; }
      to { transform: translateX(100%); opacity: 0; }
    }
  `]
})
export class ToastComponent implements OnInit, OnDestroy {
  toasts: (Toast & { exiting?: boolean })[] = [];
  private subscription!: Subscription;

  constructor(private toastService: ToastService) {}

  ngOnInit(): void {
    this.subscription = this.toastService.toasts.subscribe(toast => {
      this.toasts.push(toast);
      // Auto-remove after duration
      setTimeout(() => this.removeToast(toast.id), toast.duration || 4000);
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  removeToast(id: number): void {
    const toast = this.toasts.find(t => t.id === id);
    if (toast) {
      toast.exiting = true;
      setTimeout(() => {
        this.toasts = this.toasts.filter(t => t.id !== id);
      }, 300);
    }
  }

  getIcon(type: string): string {
    const icons: Record<string, string> = {
      'success': '✅',
      'error': '❌',
      'warning': '⚠️',
      'info': 'ℹ️'
    };
    return icons[type] || 'ℹ️';
  }
}
