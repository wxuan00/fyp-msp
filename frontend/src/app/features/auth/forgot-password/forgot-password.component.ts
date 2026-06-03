import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
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

          <!-- Request reset form -->
          <ng-container *ngIf="!submitted">
            <div class="auth-card-header">
              <h2>Forgot Password</h2>
              <p>Enter your email address and we'll send you a reset link.</p>
            </div>

            <form (ngSubmit)="onSubmit()">
              <div class="form-group">
                <label>Email Address</label>
                <input
                  type="email"
                  class="form-control"
                  [(ngModel)]="email"
                  name="email"
                  placeholder="Enter your email"
                  required>
              </div>

              <button type="submit" class="btn-primary" [disabled]="loading">
                {{ loading ? 'Sending...' : 'Send Reset Link' }}
              </button>
            </form>

            <div *ngIf="errorMessage" class="error-alert">
              ⚠️ {{ errorMessage }}
            </div>
          </ng-container>

          <!-- Success state -->
          <ng-container *ngIf="submitted">
            <div class="success-state">
              <div class="success-icon">✅</div>
              <h2>Check Your Instructions</h2>
              <p>
                A password reset link has been generated. In a production environment
                this would be emailed to <strong>{{ email }}</strong>.
              </p>

              <!-- Dev mode: show the token so testers can use it immediately -->
              <div *ngIf="devToken" class="dev-token-box">
                <p class="dev-label">🛠 Developer Mode — Reset Token:</p>
                <code class="token-value">{{ devToken }}</code>
                <p class="dev-hint">
                  Copy this token and use it on the
                  <a [routerLink]="['/reset-password']" [queryParams]="{ token: devToken }">Reset Password</a>
                  page, or click the link above.
                </p>
              </div>
            </div>
          </ng-container>

          <div class="back-link">
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
      line-height: 1.5;
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

    .form-control {
      width: 100%;
      padding: 12px 16px;
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

    .error-alert {
      margin-top: 20px;
      padding: 12px 16px;
      background: #fff5f8;
      color: #f64e60;
      border-radius: 8px;
      font-size: 0.85rem;
      font-weight: 500;
    }

    .success-state {
      text-align: center;
      padding: 8px 0 24px;
    }

    .success-icon {
      font-size: 3rem;
      margin-bottom: 16px;
    }

    .success-state h2 {
      font-size: 1.4rem;
      font-weight: 700;
      color: #111;
      margin-bottom: 12px;
    }

    .success-state p {
      color: #555;
      font-size: 0.9rem;
      line-height: 1.6;
      margin-bottom: 0;
    }

    .dev-token-box {
      margin-top: 24px;
      background: #f0f4ff;
      border: 1px solid #c5d5ff;
      border-radius: 8px;
      padding: 16px;
      text-align: left;
    }

    .dev-label {
      font-size: 0.8rem;
      font-weight: 600;
      color: #3b57d8;
      margin-bottom: 8px;
    }

    .token-value {
      display: block;
      word-break: break-all;
      font-size: 0.78rem;
      background: #fff;
      padding: 8px 10px;
      border-radius: 6px;
      border: 1px solid #d0daff;
      color: #222;
      margin-bottom: 10px;
    }

    .dev-hint {
      font-size: 0.8rem;
      color: #555;
      margin: 0;
    }

    .dev-hint a {
      color: #3b57d8;
      font-weight: 600;
      text-decoration: none;
    }

    .dev-hint a:hover { text-decoration: underline; }

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
export class ForgotPasswordComponent {
  email = '';
  loading = false;
  submitted = false;
  errorMessage = '';
  devToken: string | null = null;

  constructor(private authService: AuthService) {}

  onSubmit() {
    this.loading = true;
    this.errorMessage = '';

    this.authService.forgotPassword(this.email.trim().toLowerCase()).subscribe({
      next: (res) => {
        this.loading = false;
        this.submitted = true;
        // Dev mode: backend returns resetToken for testing
        this.devToken = res?.resetToken ?? null;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Something went wrong. Please try again.';
      }
    });
  }
}
