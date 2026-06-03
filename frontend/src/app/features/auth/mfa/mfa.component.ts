import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-mfa',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './mfa.component.html',
  styleUrls: ['./mfa.component.css']
})
export class MfaComponent implements OnInit {
  otpCode: string = '';
  errorMessage: string = '';
  loading = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // If no MFA pending, redirect appropriately
    if (!this.authService.isMfaPending()) {
      if (this.authService.getToken()) {
        this.router.navigate(['/dashboard']);
      } else {
        this.router.navigate(['/login']);
      }
    }
  }

  verifyOtp() {
    if (this.otpCode.length !== 6) {
      this.errorMessage = 'Please enter a 6-digit code';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.verifyMfa(this.otpCode).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.success) {
          this.authService.clearMfaPending();
          // Load permissions then navigate
          this.authService.getCurrentUser().subscribe({
            next: () => this.router.navigate(['/dashboard']),
            error: () => this.router.navigate(['/dashboard'])
          });
        } else {
          this.errorMessage = res.message || 'Verification failed. Please try again.';
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Verification failed. Please try again.';
      }
    });
  }

  cancel() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
