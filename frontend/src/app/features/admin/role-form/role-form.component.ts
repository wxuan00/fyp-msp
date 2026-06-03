import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { RoleService } from '../../../core/services/role.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-role-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ConfirmDialogComponent],
  templateUrl: './role-form.component.html',
  styleUrls: ['./role-form.component.css']
})
export class RoleFormComponent implements OnInit {
  isEditMode = false;
  roleId: number | null = null;

  errorMessage = '';

  // Delete dialog
  showDeleteDialog = false;

  formData = {
    roleName: '',
    roleType: '',
    description: ''
  };

  // Permissions
  allPermissions: any[] = [];
  selectedPermissionIds = new Set<number>();
  loadingPermissions = true;
  expandedModules: Record<string, boolean> = {};

  readonly moduleIcons: Record<string, string> = {
    USER: '👤', MERCHANT: '🏪', TRANSACTION: '💳',
    SETTLEMENT: '📄', REFUND: '↩️', REPORT: '📊',
    CREDIT_ADVICE: '📋', ANALYTICS: '📈', ADMIN: '🛡️'
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private roleService: RoleService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode = true;
      this.roleId = +idParam;
      forkJoin([
        this.roleService.getRoleById(this.roleId),
        this.roleService.getAllPermissions(),
        this.roleService.getRolePermissions(this.roleId)
      ]).subscribe({
        next: ([role, perms, assigned]) => {
          this.formData = { roleName: role.roleName, roleType: role.roleType || '', description: role.description || '' };
          this.allPermissions = perms;
          this.selectedPermissionIds = new Set(assigned.map((p: any) => p.permissionId));
          this.initExpandedModules();
          this.loadingPermissions = false;
        },
        error: () => { this.errorMessage = 'Error loading role'; this.loadingPermissions = false; }
      });
    } else {
      const typeParam = this.route.snapshot.queryParamMap.get('type') || 'SYSTEM';
      this.formData.roleType = typeParam;
      this.roleService.getAllPermissions().subscribe({
        next: (perms) => {
          this.allPermissions = perms;
          this.initExpandedModules();
          this.loadingPermissions = false;
        },
        error: () => { this.loadingPermissions = false; }
      });
    }
  }

  initExpandedModules() {
    this.permissionModules.forEach(m => this.expandedModules[m] = true);
  }

  get permissionModules(): string[] {
    return [...new Set(this.allPermissions.map(p => p.module || 'OTHER'))];
  }

  getPermsByModule(module: string): any[] {
    return this.allPermissions.filter(p => (p.module || 'OTHER') === module);
  }

  getModuleSelected(module: string): number {
    return this.getPermsByModule(module).filter(p => this.selectedPermissionIds.has(p.permissionId)).length;
  }

  getModuleTotal(module: string): number {
    return this.getPermsByModule(module).length;
  }

  isSelected(id: number): boolean {
    return this.selectedPermissionIds.has(id);
  }

  togglePermission(id: number) {
    if (this.selectedPermissionIds.has(id)) {
      this.selectedPermissionIds.delete(id);
    } else {
      this.selectedPermissionIds.add(id);
    }
  }

  toggleModule(module: string) {
    this.expandedModules[module] = !this.expandedModules[module];
  }

  selectAll() {
    this.allPermissions.forEach(p => this.selectedPermissionIds.add(p.permissionId));
  }

  clearAll() {
    this.selectedPermissionIds.clear();
  }

  onSubmit() {
    this.errorMessage = '';
    this.errorMessage = '';
    if (!this.formData.roleName?.trim()) {
      this.errorMessage = 'Role name is required.';
      return;
    }
    const permIds = Array.from(this.selectedPermissionIds);

    if (this.isEditMode && this.roleId) {
      this.roleService.updateRole(this.roleId, this.formData).subscribe({
        next: (updatedRole) => {
          this.roleService.updateRolePermissions(updatedRole.roleId, permIds).subscribe({
            next: () => {
              this.toast.success('Role updated successfully');
              setTimeout(() => this.router.navigate(['/roles']), 1000);
            },
            error: () => this.toast.error('Role saved but permissions update failed')
          });
        },
        error: (err) => this.toast.error(err.error?.message || 'Error updating role')
      });
    } else {
      this.roleService.createRole(this.formData).subscribe({
        next: (createdRole) => {
          this.roleService.updateRolePermissions(createdRole.roleId, permIds).subscribe({
            next: () => {
              this.toast.success('Role created successfully');
              setTimeout(() => this.router.navigate(['/roles']), 1000);
            },
            error: () => this.toast.error('Role created but permissions assignment failed')
          });
        },
        error: (err) => this.toast.error(err.error?.message || 'Error creating role')
      });
    }
  }

  confirmDelete() {
    this.showDeleteDialog = true;
  }

  onDeleteConfirmed() {
    if (this.roleId) {
      this.roleService.deleteRole(this.roleId).subscribe({
        next: () => {
          this.toast.success('Role deleted successfully');
          this.router.navigate(['/roles']);
        },
        error: (err) => this.toast.error(err.error?.message || 'Failed to delete role')
      });
    }
    this.showDeleteDialog = false;
  }

  cancel() {
    this.router.navigate(['/roles']);
  }
}
