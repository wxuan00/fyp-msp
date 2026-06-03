import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-view-profile',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './view-profile.component.html',
  styleUrls: ['./view-profile.component.css']
})
export class ViewProfileComponent implements OnInit {

  user: any = null;
  loading = true;
  
  // MFA Setup
  mfaStatus: any = null;
  showMfaSetup = false;
  mfaSetupData: any = null;
  mfaVerifyCode = '';
  mfaLoading = false;
  mfaMessage = '';
  mfaError = '';

  // MFA Disable
  showMfaDisable = false;
  disablePassword = '';
  disableCode = '';

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.loadProfile();
    this.loadMfaStatus();
  }

  loadProfile(): void {
    this.authService.getProfile().subscribe({
      next: (data) => {
        this.user = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading profile', err);
        this.loading = false;
      }
    });
  }

  loadMfaStatus(): void {
    this.authService.getMfaStatus().subscribe({
      next: (data) => {
        this.mfaStatus = data;
      },
      error: (err) => {
        console.error('Error loading MFA status', err);
      }
    });
  }

  // Start MFA setup process
  startMfaSetup(): void {
    this.mfaLoading = true;
    this.mfaMessage = '';
    this.mfaError = '';

    this.authService.setupMfa().subscribe({
      next: (data) => {
        this.mfaSetupData = data;
        this.showMfaSetup = true;
        this.mfaLoading = false;
      },
      error: (err) => {
        this.mfaError = err.error?.message || 'Failed to start MFA setup';
        this.mfaLoading = false;
      }
    });
  }

  // Verify and enable MFA
  enableMfa(): void {
    if (this.mfaVerifyCode.length !== 6) {
      this.mfaError = 'Please enter a 6-digit code';
      return;
    }

    this.mfaLoading = true;
    this.mfaError = '';

    this.authService.enableMfa(this.mfaSetupData.secret, this.mfaVerifyCode).subscribe({
      next: (res) => {
        this.mfaLoading = false;
        if (res.success) {
          this.mfaMessage = 'MFA enabled successfully!';
          this.showMfaSetup = false;
          this.mfaSetupData = null;
          this.mfaVerifyCode = '';
          this.loadMfaStatus();
          this.loadProfile();
        } else {
          this.mfaError = res.message || 'Failed to enable MFA';
        }
      },
      error: (err) => {
        this.mfaLoading = false;
        this.mfaError = err.error?.message || 'Failed to enable MFA';
      }
    });
  }

  cancelMfaSetup(): void {
    this.showMfaSetup = false;
    this.mfaSetupData = null;
    this.mfaVerifyCode = '';
    this.mfaError = '';
  }

  // Start MFA disable process
  startMfaDisable(): void {
    this.showMfaDisable = true;
    this.disablePassword = '';
    this.disableCode = '';
    this.mfaError = '';
  }

  // Disable MFA
  disableMfa(): void {
    if (!this.disablePassword) {
      this.mfaError = 'Please enter your password';
      return;
    }
    if (this.disableCode.length !== 6) {
      this.mfaError = 'Please enter a 6-digit code';
      return;
    }

    this.mfaLoading = true;
    this.mfaError = '';

    this.authService.disableMfa(this.disablePassword, this.disableCode).subscribe({
      next: (res) => {
        this.mfaLoading = false;
        if (res.success) {
          this.mfaMessage = 'MFA disabled successfully';
          this.showMfaDisable = false;
          this.disablePassword = '';
          this.disableCode = '';
          this.loadMfaStatus();
          this.loadProfile();
        } else {
          this.mfaError = res.message || 'Failed to disable MFA';
        }
      },
      error: (err) => {
        this.mfaLoading = false;
        this.mfaError = err.error?.message || 'Failed to disable MFA';
      }
    });
  }

  cancelMfaDisable(): void {
    this.showMfaDisable = false;
    this.disablePassword = '';
    this.disableCode = '';
    this.mfaError = '';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('en-MY', {
      year: 'numeric', month: 'short', day: 'numeric'
    });
  }
}
