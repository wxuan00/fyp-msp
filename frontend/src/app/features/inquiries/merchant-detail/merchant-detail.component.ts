import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MerchantService } from '../../../core/services/merchant.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-merchant-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './merchant-detail.component.html',
  styleUrls: ['./merchant-detail.component.css']
})
export class MerchantDetailComponent implements OnInit {
  merchant: any = null;
  loading = true;
  merchantId: number = 0;
  isAdmin = false;
  canManageUsers = false;

  // User mapping
  mappedUsers: any[] = [];
  usersLoading = false;
  userSearchQuery = '';
  searchResults: any[] = [];
  searching = false;
  private searchSubject = new Subject<string>();

  // Delete
  showDeleteDialog = false;
  deleting = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private merchantService: MerchantService,
    private authService: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    this.canManageUsers = this.isAdmin || this.authService.hasPermission('MANAGE_USERS') || this.authService.hasPermission('MANAGE_CHILD_USERS');
    this.merchantId = +this.route.snapshot.paramMap.get('id')!;
    this.merchantService.getMerchantById(this.merchantId).subscribe({
      next: (data) => { this.merchant = data; this.loading = false; },
      error: () => { this.loading = false; }
    });

    if (this.canManageUsers) {
      this.loadMappedUsers();
      // Debounce user search input
      this.searchSubject.pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(q => {
          this.searching = true;
          return this.merchantService.searchUsers(q);
        })
      ).subscribe({
        next: (results) => {
          // Exclude already-mapped users
          const mappedIds = new Set(this.mappedUsers.map(u => u.userId));
          this.searchResults = results.filter(u => !mappedIds.has(u.userId));
          this.searching = false;
        },
        error: () => { this.searching = false; }
      });
    }
  }

  loadMappedUsers() {
    this.usersLoading = true;
    this.merchantService.getMerchantUsers(this.merchantId).subscribe({
      next: (users) => { this.mappedUsers = users; this.usersLoading = false; },
      error: () => { this.usersLoading = false; }
    });
  }

  onUserSearch(query: string) {
    this.searchResults = [];
    if (query.trim().length < 2) return;
    this.searchSubject.next(query.trim());
  }

  assignUser(user: any) {
    this.merchantService.assignUserToMerchant(this.merchantId, user.userId).subscribe({
      next: () => {
        this.toast.success(`${user.firstName} ${user.lastName} assigned to merchant`);
        this.userSearchQuery = '';
        this.searchResults = [];
        this.loadMappedUsers();
      },
      error: (err) => this.toast.error(err.error?.error || 'Failed to assign user')
    });
  }

  removeUser(user: any) {
    this.merchantService.removeUserFromMerchant(this.merchantId, user.userId).subscribe({
      next: () => {
        this.toast.success(`${user.firstName} ${user.lastName} removed from merchant`);
        this.loadMappedUsers();
      },
      error: (err) => this.toast.error(err.error?.error || 'Failed to remove user')
    });
  }

  displayName(u: any): string {
    return u.displayName || `${u.firstName} ${u.lastName}`;
  }

  openDeleteDialog() { this.showDeleteDialog = true; }
  dismissDeleteDialog() { this.showDeleteDialog = false; }

  confirmDelete() {
    this.deleting = true;
    this.merchantService.deleteMerchant(this.merchantId).subscribe({
      next: () => {
        this.toast.success('Merchant deleted successfully.');
        this.router.navigate(['/merchants']);
      },
      error: () => {
        this.deleting = false;
        this.toast.error('Failed to delete merchant.');
      }
    });
  }
}
