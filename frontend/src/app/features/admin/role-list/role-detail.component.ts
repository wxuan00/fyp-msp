import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { RoleService } from '../../../core/services/role.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-role-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ConfirmDialogComponent],
  templateUrl: './role-detail.component.html',
  styleUrls: ['./role-detail.component.css']
})
export class RoleDetailComponent implements OnInit {
  role: any = null;
  loading = true;
  error = '';

  showDeleteDialog = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private roleService: RoleService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.roleService.getAllRolesWithPermissions().subscribe({
        next: (roles) => {
          this.role = roles.find(r => r.roleId === +id) || null;
          if (!this.role) this.error = 'Role not found.';
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load role details.';
          this.loading = false;
        }
      });
    }
  }

  getPermsByModule(): { module: string; perms: any[] }[] {
    const perms: any[] = this.role?.permissions || [];
    const map: { [m: string]: any[] } = {};
    for (const p of perms) {
      const mod = p.module || 'OTHER';
      if (!map[mod]) map[mod] = [];
      map[mod].push(p);
    }
    return Object.keys(map).sort().map(m => ({ module: m, perms: map[m] }));
  }

  goBack() {
    this.router.navigate(['/roles']);
  }

  confirmDelete() {
    this.showDeleteDialog = true;
  }

  onDeleteConfirmed() {
    this.roleService.deleteRole(this.role.roleId).subscribe({
      next: () => {
        this.toast.success('Role deleted successfully');
        this.router.navigate(['/roles']);
      },
      error: (err) => this.toast.error(err.error?.message || 'Failed to delete role')
    });
    this.showDeleteDialog = false;
  }

  onDeleteCancelled() {
    this.showDeleteDialog = false;
  }
}
