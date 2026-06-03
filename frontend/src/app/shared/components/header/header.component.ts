import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { SidebarComponent } from '../sidebar/sidebar.component';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {

  @Input() sidebarRef?: SidebarComponent;
  userName: string = '';
  userRole: string = '';

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.userRole = this.authService.getUserRole();
    this.authService.getProfile().subscribe({
      next: (data) => {
        this.userName = data.displayName || (data.firstName ? `${data.firstName} ${data.lastName}` : data.email);
      },
      error: () => {
        this.userName = 'User';
      }
    });
  }

  toggleSidebar(): void {
    if (this.sidebarRef) {
      this.sidebarRef.toggleSidebar();
    }
  }
}
