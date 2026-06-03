import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="auth-page">
      <div class="auth-left">
        <div class="brand-area">
          <div class="brand-logo">🏦</div>
          <h1>Merchant Service Portal</h1>
          <p>Manage your merchant accounts, transactions, and business operations in one secure platform.</p>
        </div>
      </div>

      <div class="auth-right">
        <div class="auth-card">

          <!-- Invalid / missing token -->
          <ng-container *ngIf="!token">
            <div class="error-state">
              <div class="error-icon">❌</div>
              <h2>Invalid Reset Link</h2>
              <p>This password reset link is invalid or has expired. Please request a new one.</p>
              <a routerLink="/forgot-password" class="btn-primary inline-btn">Request New Link</a>
            </div>
          </ng-container>

          <!-- Reset form -->
          <ng-container *ngIf="token && !success">
            <div class="auth-card-header">
              <h2>Reset Password</h2>
              <p>Enter your new password below.</p>
            </div>

            <form (ngSubmit)="onSubmit()">
              <div class="form-group">
                <label>New Password</label>
                <div class="password-wrapper">
                  <input
                    [type]="showNew ? 'text' : 'password'"
                    class="form-control"
                    [(ngModel)]="newPassword"
                    name="newPassword"
                    placeholder="Enter new password"
                    required>
                </div>
              </div>

              <div class="form-group">
                <label>Confirm New Password</label>
                <div class="password-wrapper">
                  <input
                    [type]="showConfirm ? 'text' : 'password'"
                    class="form-control"
                    [(ngModel)]="confirmPassword"
                    name="confirmPassword"
                    placeholder="Confirm new password"
                    required>
                </div>
                <div *ngIf="confirmPassword && confirmPassword === newPassword" class="field-success">
                  &#10003; Passwords match
                </div>
                <div *ngIf="confirmPassword && newPassword !== confirmPassword" class="field-error">
                  &#10007; Passwords do not match.
                </div>
              </div>

              <button
                type="submit"
                class="btn-primary"
                [disabled]="loading || !newPassword || newPassword !== confirmPassword">
                {{ loading ? 'Resetting...' : 'Reset Password' }}
              </button>
            </form>

            <div *ngIf="errorMessage" class="error-alert">
              ⚠️ {{ errorMessage }}
            </div>
          </ng-container>

          <!-- Success state -->
          <ng-container *ngIf="success">
            <div class="success-state">
              <div class="success-icon">✅</div>
              <h2>Password Reset!</h2>
              <p>Your password has been reset successfully. You can now sign in with your new password.</p>
              <p class="redirect-hint">Redirecting to login in {{ countdown }}s...</p>
              <a routerLink="/login" class="btn-primary inline-btn">Go to Sign In</a>
            </div>
          </ng-container>

          <div class="back-link" *ngIf="!success">
            <a routerLink="/login">← Back to Sign In</a>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-page {
      display: flex;
      min-height: 100vh;
      background: #f5f5f5;
    }

    .auth-left {
      flex: 1;
      background: #111;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 60px;
    }

    .brand-area {
      max-width: 420px;
      color: #fff;
      text-align: center;
    }

    .brand-logo {
      font-size: 3.5rem;
      margin-bottom: 24px;
    }

    .brand-area h1 {
      font-size: 2rem;
      font-weight: 700;
      margin-bottom: 16px;
      letter-spacing: -0.5px;
    }

    .brand-area p {
      font-size: 1rem;
      line-height: 1.7;
      color: rgba(255,255,255,0.75);
    }

    .auth-right {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 40px;
    }

    .auth-card {
      width: 100%;
      max-width: 420px;
      background: #fff;
      border-radius: 12px;
      padding: 40px;
      box-shadow: 0 4px 24px rgba(0,0,0,0.08);
    }

    .auth-card-header {
      margin-bottom: 32px;
    }

    .auth-card-header h2 {
      font-size: 1.6rem;
      font-weight: 700;
      color: #111;
      margin-bottom: 6px;
    }

    .auth-card-header p {
      color: #888;
      font-size: 0.9rem;
    }

    .form-group {
      margin-bottom: 20px;
    }

    .form-group label {
      display: block;
      font-weight: 600;
      font-size: 0.82rem;
      color: #1a1a1a;
      margin-bottom: 8px;
    }

    .password-wrapper {
      position: relative;
    }

    .form-control {
      width: 100%;
      padding: 12px 44px 12px 16px;
      border: 1px solid #ddd;
      border-radius: 8px;
      font-size: 0.9rem;
      font-family: inherit;
      color: #1a1a1a;
      transition: border-color 0.2s;
      box-sizing: border-box;
    }

    .form-control:focus {
      outline: none;
      border-color: #111;
      box-shadow: 0 0 0 3px rgba(0,0,0,0.06);
    }

    .form-control::placeholder {
      color: #b5b5c3;
    }

    .toggle-pw {
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
      background: none;
      border: none;
      cursor: pointer;
      font-size: 1rem;
      padding: 0;
      line-height: 1;
    }

    .field-error {
      margin-top: 6px;
      font-size: 0.8rem;
      color: #f64e60;
      font-weight: 500;
    }

    .btn-primary {
      width: 100%;
      padding: 13px;
      background: #111;
      color: #fff;
      border: none;
      border-radius: 8px;
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
      margin-top: 8px;
      font-family: inherit;
    }

    .btn-primary:hover { background: #333; }
    .btn-primary:disabled { background: #bbb; cursor: not-allowed; }

    .inline-btn {
      display: inline-block;
      width: auto;
      padding: 12px 28px;
      text-decoration: none;
      margin-top: 20px;
      border-radius: 8px;
    }

    .error-alert {
      margin-top: 20px;
      padding: 12px 16px;
      background: #fff5f8;
      color: #f64e60;
      border-radius: 8px;
      font-size: 0.85rem;
      font-weight: 500;
    }

    .success-state,
    .error-state {
      text-align: center;
      padding: 8px 0 24px;
    }

    .success-icon,
    .error-icon {
      font-size: 3rem;
      margin-bottom: 16px;
    }

    .success-state h2,
    .error-state h2 {
      font-size: 1.4rem;
      font-weight: 700;
      color: #111;
      margin-bottom: 12px;
    }

    .success-state p,
    .error-state p {
      color: #555;
      font-size: 0.9rem;
      line-height: 1.6;
      margin-bottom: 6px;
    }

    .redirect-hint {
      font-size: 0.82rem;
      color: #888;
    }

    .back-link {
      margin-top: 24px;
      text-align: center;
    }

    .back-link a {
      color: #555;
      font-size: 0.88rem;
      text-decoration: none;
      font-weight: 500;
    }

    .back-link a:hover { color: #111; }

    @media (max-width: 900px) {
      .auth-left { display: none; }
      .auth-right { padding: 20px; }
    }
  `]
})
export class ResetPasswordComponent implements OnInit {
  token: string | null = null;
  newPassword = '';
  confirmPassword = '';
  loading = false;
  success = false;
  errorMessage = '';
  showNew = false;
  showConfirm = false;
  countdown = 5;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit() {
    // Support both query param (?token=...) and route param (:token)
    this.token = this.route.snapshot.queryParamMap.get('token')
      || this.route.snapshot.paramMap.get('token')
      || null;
  }

  onSubmit() {
    if (!this.token || this.newPassword !== this.confirmPassword) return;

    this.loading = true;
    this.errorMessage = '';

    this.authService.resetPassword(this.token, this.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.success = true;
        this.startCountdown();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Something went wrong. Please try again.';
      }
    });
  }

  private startCountdown() {
    const interval = setInterval(() => {
      this.countdown--;
      if (this.countdown <= 0) {
        clearInterval(interval);
        this.router.navigate(['/login']);
      }
    }, 1000);
  }
}
