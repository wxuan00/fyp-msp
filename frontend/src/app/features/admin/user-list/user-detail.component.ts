import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { RoleService } from '../../../core/services/role.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-user-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ConfirmDialogComponent],
  templateUrl: './user-detail.component.html',
  styleUrls: ['./user-detail.component.css']
})
export class UserDetailComponent implements OnInit {
  user: any = null;
  loading = true;
  error = '';
  canManageThisUser = false;
  canViewRoles = false;

  // Delete user dialog
  showDeleteDialog = false;
  deleteLabel = '';

  // Unassign role dialog
  showUnassignDialog = false;
  unassignRoleId: number | null = null;
  unassignRoleName = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private roleService: RoleService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.loadUser();
  }

  loadUser(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.authService.getUserDetails(+id).subscribe({
        next: (data) => {
          this.user = data;
          this.loading = false;
          // Determine if current user can manage this user
          const isAdmin = this.authService.isAdmin();
          const hasManageUsers = this.authService.hasPermission('MANAGE_USERS');
          const hasManageChildUsers = this.authService.hasPermission('MANAGE_CHILD_USERS');
          this.canViewRoles = this.authService.hasPermission('MANAGE_ROLES');
          const currentEmail = this.authService.getCurrentUserEmail();
          // Never allow edit/delete on own account
          if (this.user.email === currentEmail) {
            this.canManageThisUser = false;
          // Never allow edit/delete on System Admin (super admin with no createdBy)
          } else if (this.user.role === 'ADMIN' && (!this.user.createdBy || this.user.createdBy === '')) {
            this.canManageThisUser = false;
          } else if (isAdmin || hasManageUsers) {
            this.canManageThisUser = true;
          } else if (hasManageChildUsers) {
            this.canManageThisUser = this.user.createdBy === currentEmail;
          }
        },
        error: () => {
          this.error = 'Failed to load user details.';
          this.loading = false;
        }
      });
    }
  }

  goBack() {
    this.router.navigate(['/users']);
  }

  goEdit() {
    this.router.navigate(['/users', this.user.userId, 'edit']);
  }

  // Delete user
  confirmDeleteUser() {
    const fullName = (this.user.firstName + ' ' + this.user.lastName).trim();
    this.deleteLabel = this.user.displayName?.trim() || fullName || this.user.email;
    this.showDeleteDialog = true;
  }

  onDeleteConfirmed() {
    this.authService.deleteUser(this.user.userId).subscribe({
      next: () => {
        this.toast.success('User deleted successfully');
        this.router.navigate(['/users']);
      },
      error: () => this.toast.error('Failed to delete user')
    });
    this.showDeleteDialog = false;
  }

  onDeleteCancelled() {
    this.showDeleteDialog = false;
  }

  // Unassign role
  confirmUnassign(roleId: number, roleName: string) {
    this.unassignRoleId = roleId;
    this.unassignRoleName = roleName;
    this.showUnassignDialog = true;
  }

  onUnassignConfirmed() {
    if (this.unassignRoleId !== null) {
      this.roleService.unassignUserRole(this.user.userId, this.unassignRoleId).subscribe({
        next: () => {
          this.toast.success(`Role "${this.unassignRoleName}" removed`);
          this.loadUser();
        },
        error: () => this.toast.error('Failed to remove role')
      });
    }
    this.showUnassignDialog = false;
    this.unassignRoleId = null;
  }

  onUnassignCancelled() {
    this.showUnassignDialog = false;
    this.unassignRoleId = null;
  }
}
