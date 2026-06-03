import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { MerchantService } from '../../../core/services/merchant.service';
import { RoleService } from '../../../core/services/role.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';


@Component({
  selector: 'app-user-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ConfirmDialogComponent],
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.css']
})
export class UserFormComponent implements OnInit {
  isEditMode = false;
  userId: number | null = null;
  merchants: any[] = [];
  allRoles: any[] = [];
  selectedRoleIds = new Set<number>();
  message = '';
  errorMessage = '';
  errors: Record<string, string> = {};

  formErrorMessage = '';

  /** Locked user type determined from query param on create ('ADMIN' | 'MERCHANT') */
  userType: 'ADMIN' | 'MERCHANT' = 'MERCHANT';

  /** True when the logged-in creator is a merchant user — locks merchant selection to their own merchants */
  isCreatorMerchantUser = false;
  /** Merchants the current logged-in user belongs to (used when isCreatorMerchantUser=true) */
  creatorMerchants: any[] = [];

  // Merchant mapping (create mode, MERCHANT type only)
  merchantSearchQuery = '';
  merchantSearchResults: any[] = [];
  selectedMerchant: any = null;
  showMerchantDropdown = false;
  /** Multi-merchant selection for new user */
  selectedMerchants: any[] = [];

  /** Returns merchants available in dropdown (excluding already-selected ones) */
  get availableMerchantResults(): any[] {
    const selectedIds = new Set(this.selectedMerchants.map(m => m.merchantId));
    return this.merchantSearchResults.filter(m => !selectedIds.has(m.merchantId));
  }

  // Delete dialog
  showDeleteDialog = false;
  deleteLabel = '';

  formData: any = {
    firstName: '',
    lastName: '',
    email: '',
    contactNumber: '',
    displayName: '',
    role: 'MERCHANT',
    status: 'ACTIVE'
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private merchantService: MerchantService,
    private roleService: RoleService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.merchantService.getAllMerchants().subscribe({
      next: (data) => this.merchants = data,
      error: () => {}
    });

    this.roleService.getAllRolesWithPermissions().subscribe({
      next: (data) => this.allRoles = data,
      error: () => {}
    });

    // On create: lock user type from query param (ADMIN = Bank User, MERCHANT = Merchant User)
    const typeParam = this.route.snapshot.queryParamMap.get('type');
    if (typeParam === 'ADMIN' || typeParam === 'MERCHANT') {
      this.userType = typeParam;
      this.formData.role = typeParam;
    }

    // Detect if current logged-in user is a merchant user
    if (!this.authService.isAdmin()) {
      this.isCreatorMerchantUser = true;
      this.merchantService.getMyMerchants().subscribe({
        next: (myMerchants) => {
          this.creatorMerchants = myMerchants;
        },
        error: () => {}
      });
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.userId = +idParam;
      this.authService.getUserDetails(this.userId).subscribe({
        next: (user) => {
          if (user) {
            this.formData = {
              firstName: user.firstName || '',
              lastName: user.lastName || '',
              email: user.email || '',
              contactNumber: user.contactNumber || '',
              displayName: user.displayName || '',
              role: user.role || 'MERCHANT',
              status: user.status || 'ACTIVE'
            };
            this.userType = (user.role === 'ADMIN' ? 'ADMIN' : 'MERCHANT');
            if (user.roles && user.roles.length > 0) {
              this.selectedRoleIds = new Set(user.roles.map((r: any) => r.roleId));
            }
            // Load linked merchants for edit mode
            if (user.linkedMerchants && user.linkedMerchants.length > 0) {
              this.selectedMerchants = user.linkedMerchants.map((m: any) => ({
                merchantId: m.merchantId,
                merchantName: m.merchantName,
                status: m.status
              }));
            }
          }
        },
        error: () => this.errorMessage = 'Error loading user details'
      });
    }
  }

  onMerchantSearch(q: string) {
    if (this.errors['merchant']) delete this.errors['merchant'];
    this.filterMerchants(q);
  }

  onMerchantFocus() {
    if (this.errors['merchant']) delete this.errors['merchant'];
    this.filterMerchants(this.merchantSearchQuery);
    this.showMerchantDropdown = true;
  }

  onMerchantBlur() {
    // Delay hiding so click on dropdown item registers first
    setTimeout(() => { this.showMerchantDropdown = false; }, 200);
  }

  private filterMerchants(q: string) {
    const term = q.trim().toLowerCase();
    // Merchant creators can only link to their own merchants
    const source = this.isCreatorMerchantUser ? this.creatorMerchants : this.merchants;
    if (!term) {
      this.merchantSearchResults = source;
    } else {
      this.merchantSearchResults = source.filter(m =>
        m.merchantName?.toLowerCase().includes(term) ||
        String(m.merchantId).includes(term)
      );
    }
    this.showMerchantDropdown = true;
  }

  selectMerchant(m: any) {
    if (!this.selectedMerchants.find(s => s.merchantId === m.merchantId)) {
      this.selectedMerchants.push(m);
    }
    this.merchantSearchQuery = '';
    this.merchantSearchResults = [];
    this.showMerchantDropdown = false;
  }

  removeMerchant(m: any) {
    this.selectedMerchants = this.selectedMerchants.filter(s => s.merchantId !== m.merchantId);
  }

  clearMerchant() {
    this.selectedMerchant = null;
    this.selectedMerchants = [];
    this.merchantSearchQuery = '';
    this.merchantSearchResults = [];
    this.showMerchantDropdown = false;
  }

  isRoleSelected(roleId: number): boolean {
    return this.selectedRoleIds.has(roleId);
  }

  toggleRole(roleId: number) {
    if (this.selectedRoleIds.has(roleId)) {
      this.selectedRoleIds.delete(roleId);
    } else {
      this.selectedRoleIds.add(roleId);
    }
  }

  onDisplayNameBlur() {
    const name = this.formData.displayName?.trim();
    if (!name) {
      delete this.errors['displayName'];
      return;
    }
    const excludeId = this.isEditMode && this.userId ? String(this.userId) : undefined;
    this.authService.checkDisplayName(name, excludeId).subscribe({
      next: (res: any) => {
        if (res.taken) {
          this.errors['displayName'] = `Display name "${name}" is already taken.`;
        } else {
          delete this.errors['displayName'];
        }
      },
      error: () => {}
    });
  }

  get filteredRoles(): any[] {
    if (this.formData.role === 'ADMIN') {
      // Admin users: show SYSTEM roles (includes ADMIN base role and SYSTEM custom roles)
      return this.allRoles.filter(r => {
        const t = (r.roleType || '').toUpperCase();
        return t === 'SYSTEM' || t === '';
      });
    } else {
      // Merchant users: show MERCHANT system role + BUSINESS custom roles — all must be explicitly chosen
      return this.allRoles.filter(r => {
        const n = (r.roleName || '').toUpperCase();
        const t = (r.roleType || '').toUpperCase();
        return n === 'MERCHANT' || t === 'BUSINESS' || t === 'MERCHANT';
      });
    }
  }

  /** Returns true if a role should be disabled (greyed out) for merchant users.
   *  A role is disabled when it contains the MANAGE_ROLE permission and the
   *  target user is a MERCHANT-type user. */
  isRoleDisabled(role: any): boolean {
    if (this.formData.role === 'ADMIN') return false;
    const perms: any[] = role.permissions || [];
    return perms.some((p: any) => (p.permissionName || '').toUpperCase() === 'MANAGE_ROLE');
  }

  validate(): boolean {
    // Preserve async errors (e.g. displayName taken) — only re-check sync fields
    const asyncErrors: Record<string, string> = {};
    if (this.errors['displayName']) asyncErrors['displayName'] = this.errors['displayName'];

    this.errors = { ...asyncErrors };
    if (!this.formData.displayName?.trim()) this.errors['displayName'] = 'Display name is required.';
    if (!this.formData.firstName?.trim()) this.errors['firstName'] = 'First name is required.';
    if (!this.formData.lastName?.trim()) this.errors['lastName'] = 'Last name is required.';
    if (!this.formData.email?.trim()) {
      this.errors['email'] = 'Email address is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.formData.email)) {
      this.errors['email'] = 'Please enter a valid email address.';
    }
    if (this.selectedRoleIds.size === 0) {
      this.errors['roles'] = 'At least one role must be assigned.';
    }
    if (this.userType === 'MERCHANT' && this.selectedMerchants.length === 0) {
      this.errors['merchant'] = 'At least one merchant must be selected for Merchant users.';
    }
    return Object.keys(this.errors).length === 0;
  }

  onSubmit() {

    this.formErrorMessage = '';
    if (!this.validate()) return;

    if (this.isEditMode && this.userId) {
      const { ...payload } = this.formData;
      this.authService.updateUser(this.userId, payload).subscribe({
        next: () => {
          const roleIds = Array.from(this.selectedRoleIds);
          this.roleService.syncUserRoles(this.userId!, roleIds).subscribe({
            next: () => {
              // Sync merchants for merchant users
              if (this.userType === 'MERCHANT') {
                const merchantIds = this.selectedMerchants.map(m => m.merchantId);
                this.merchantService.syncUserMerchants(this.userId!, merchantIds).subscribe({
                  next: () => {
                    this.toast.success('User updated successfully');
                    setTimeout(() => this.router.navigate(['/users', this.userId, 'view']), 1000);
                  },
                  error: () => {
                    this.toast.success('User updated but merchant sync failed');
                    setTimeout(() => this.router.navigate(['/users', this.userId, 'view']), 2000);
                  }
                });
              } else {
                this.toast.success('User updated successfully');
                setTimeout(() => this.router.navigate(['/users', this.userId, 'view']), 1000);
              }
            },
            error: (err) => {
              this.toast.error('User saved but role sync failed: ' + (err.error?.message || err.message || 'unknown error'));
              setTimeout(() => this.router.navigate(['/users', this.userId, 'view']), 2000);
            }
          });
        },
        error: (err) => this.toast.error(err.error?.message || 'Error updating user')
      });
    } else {
      this.authService.createUser(this.formData).subscribe({
        next: (created) => {
          // Always include the base system role (ADMIN/MERCHANT) so userType classification
          // in getAllUsers remains correct after syncRoles wipes and re-inserts all roles.
          const baseRoleName = (this.formData.role || '').toUpperCase(); // 'ADMIN' or 'MERCHANT'
          const baseRole = this.allRoles.find(
            r => (r.roleName || '').toUpperCase() === baseRoleName
          );
          const roleIds = Array.from(this.selectedRoleIds);
          if (baseRole && !roleIds.includes(baseRole.roleId)) {
            roleIds.push(baseRole.roleId);
          }
          if (roleIds.length > 0 && created?.userId) {
            this.roleService.syncUserRoles(created.userId, roleIds).subscribe();
          }
          // Merchant mapping: link to selected merchants (both admin and merchant creators)
          if (this.selectedMerchants.length > 0 && created?.userId && this.userType === 'MERCHANT') {
            for (const m of this.selectedMerchants) {
              this.merchantService.assignUserToMerchant(m.merchantId, created.userId).subscribe({
                error: () => this.toast.error('User created but merchant mapping failed')
              });
            }
          }
          this.toast.success('User created successfully');
          setTimeout(() => this.router.navigate(['/users']), 1000);
        },
        error: (err) => this.toast.error(err.error?.message || 'Error creating user')
      });
    }
  }

  confirmDelete() {
    const fullName = (this.formData.firstName + ' ' + this.formData.lastName).trim();
    this.deleteLabel = this.formData.displayName?.trim() || fullName || this.formData.email;
    this.showDeleteDialog = true;
  }

  onDeleteConfirmed() {
    if (this.userId) {
      this.authService.deleteUser(this.userId).subscribe({
        next: () => {
          this.toast.success('User deleted successfully');
          this.router.navigate(['/users']);
        },
        error: () => this.toast.error('Failed to delete user')
      });
    }
    this.showDeleteDialog = false;
  }

  cancel() {
    this.router.navigate(['/users']);
  }
}
