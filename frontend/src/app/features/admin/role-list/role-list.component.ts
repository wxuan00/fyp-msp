import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { RoleService } from '../../../core/services/role.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-role-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, ConfirmDialogComponent],
  templateUrl: './role-list.component.html',
  styleUrls: ['./role-list.component.css']
})
export class RoleListComponent implements OnInit {
  allRoles: any[] = [];
  roles: any[] = [];
  activeTab: 'bank' | 'merchant' = 'bank';
  searchTerm = '';
  loading = true;
  loadError = false;

  showDeleteDialog = false;
  deleteTargetId: number | null = null;
  deleteTargetName = '';

  constructor(private roleService: RoleService, private toast: ToastService, private router: Router) {}

  ngOnInit(): void {
    this.fetchRoles();
  }

  fetchRoles() {
    this.loading = true;
    this.loadError = false;
    this.roleService.getAllRolesWithPermissions().subscribe({
      next: (data) => {
        this.allRoles = data;
        this.applyFilter();
        this.loading = false;
      },
      error: () => { this.loading = false; this.loadError = true; }
    });
  }

  applyFilter() {
    const term = this.searchTerm.trim().toLowerCase();
    let filtered = this.allRoles;
    if (this.activeTab === 'bank') {
      filtered = filtered.filter(r => {
        const type = (r.roleType || '').toUpperCase();
        return type === 'SYSTEM' || type === '';
      });
    } else {
      filtered = filtered.filter(r => {
        const type = (r.roleType || '').toUpperCase();
        return type === 'BUSINESS' || type === 'MERCHANT';
      });
    }
    if (term) {
      filtered = filtered.filter(r =>
        (r.roleName || '').toLowerCase().includes(term)
      );
    }
    this.roles = filtered;
  }

  clearFilters() {
    this.searchTerm = '';
    this.applyFilter();
  }

  switchTab(tab: 'bank' | 'merchant') {
    this.activeTab = tab;
    this.searchTerm = '';
    this.applyFilter();
  }

  get bankRoleCount(): number {
    return this.allRoles.filter(r => {
      const type = (r.roleType || '').toUpperCase();
      return type === 'SYSTEM' || type === '';
    }).length;
  }

  get merchantRoleCount(): number {
    return this.allRoles.filter(r => {
      const type = (r.roleType || '').toUpperCase();
      return type === 'BUSINESS' || type === 'MERCHANT';
    }).length;
  }

  goToCreateRole() {
    const type = this.activeTab === 'bank' ? 'SYSTEM' : 'BUSINESS';
    this.router.navigate(['/roles', 'new'], { queryParams: { type } });
  }

  deleteRole(id: number, name: string = '') {
    this.deleteTargetId = id;
    this.deleteTargetName = name;
    this.showDeleteDialog = true;
  }

  confirmDeleteRole() {
    if (this.deleteTargetId !== null) {
      this.roleService.deleteRole(this.deleteTargetId).subscribe({
        next: () => {
          this.toast.success('Role deleted successfully');
          this.fetchRoles();
        },
        error: (err) => this.toast.error(err.error?.message || 'Cannot delete this role')
      });
    }
    this.showDeleteDialog = false;
  }

  cancelDeleteRole() {
    this.showDeleteDialog = false;
    this.deleteTargetId = null;
  }
}
