import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, ConfirmDialogComponent],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})

export class SidebarComponent implements OnInit {

  userRole: string = '';
  collapsed = false;
  mobileOpen = false;
  showLogoutDialog = false;

  constructor(
    private router: Router,
    public authService: AuthService
  ) {}

  ngOnInit() {
    this.userRole = this.authService.getUserRole();
    this.checkScreenSize();
  }

  @HostListener('window:resize')
  onResize() {
    this.checkScreenSize();
  }

  checkScreenSize() {
    if (window.innerWidth <= 768) {
      this.collapsed = true;
      this.mobileOpen = false;
    } else if (window.innerWidth <= 1200) {
      this.collapsed = true;
    } else {
      this.collapsed = false;
    }
  }

  toggleSidebar() {
    if (window.innerWidth <= 768) {
      this.mobileOpen = !this.mobileOpen;
    } else {
      this.collapsed = !this.collapsed;
    }
  }

  closeMobileMenu() {
    if (window.innerWidth <= 768) {
      this.mobileOpen = false;
    }
  }

  confirmLogout() {
    this.showLogoutDialog = true;
  }

  doLogout() {
    this.showLogoutDialog = false;
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  cancelLogout() {
    this.showLogoutDialog = false;
  }
}

