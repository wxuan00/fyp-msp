import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="login-page">
      <div class="login-left">
        <div class="brand-area">
          <div class="brand-logo">🏦</div>
          <h1>Merchant Service Portal</h1>
          <p>Manage your merchant accounts, transactions, and business operations in one secure platform.</p>
        </div>
      </div>

      <div class="login-right">
        <div class="login-card">
          <div class="login-card-header">
            <h2>Welcome Back</h2>
            <p>Sign in to your account</p>
          </div>

          <form (ngSubmit)="onLogin()">
            <div class="form-group">
              <label>Email or Display Name</label>
              <input 
                type="text" 
                class="form-control" 
                [(ngModel)]="identifier" 
                name="identifier" 
                placeholder="Enter your email or display name"
                required>
            </div>

            <div class="form-group">
              <label>Password</label>
              <input 
                type="password" 
                class="form-control" 
                [(ngModel)]="password" 
                name="password" 
                placeholder="Enter your password"
                required>
            </div>

            <button type="submit" class="btn-login" [disabled]="loading">
              {{ loading ? 'Signing in...' : 'Sign In' }}
            </button>

            <div class="forgot-link">
              <a routerLink="/forgot-password">Forgot password?</a>
            </div>
          </form>

          <div *ngIf="sessionExpired" class="inactivity-alert">
            🔒 You were logged out due to 10 minutes of inactivity.
          </div>

          <div *ngIf="errorMessage" class="error-alert">
            ⚠️ {{ errorMessage }}
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-page {
      display: flex;
      min-height: 100vh;
      background: #f5f5f5;
    }

    .login-left {
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

    .login-right {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 40px;
    }

    .login-card {
      width: 100%;
      max-width: 420px;
      background: #fff;
      border-radius: 12px;
      padding: 40px;
      box-shadow: 0 4px 24px rgba(0,0,0,0.08);
    }

    .login-card-header {
      margin-bottom: 32px;
    }

    .login-card-header h2 {
      font-size: 1.6rem;
      font-weight: 700;
      color: #111;
      margin-bottom: 6px;
    }

    .login-card-header p {
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

    .btn-login {
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

    .btn-login:hover { background: #333; }
    .btn-login:disabled { background: #bbb; cursor: not-allowed; }

    .forgot-link {
      margin-top: 14px;
      text-align: center;
    }

    .forgot-link a {
      color: #555;
      font-size: 0.85rem;
      text-decoration: none;
      font-weight: 500;
    }

    .forgot-link a:hover { color: #111; text-decoration: underline; }

    .error-alert {
      margin-top: 20px;
      padding: 12px 16px;
      background: #fff5f8;
      color: #f64e60;
      border-radius: 8px;
      font-size: 0.85rem;
      font-weight: 500;
    }

    .inactivity-alert {
      margin-top: 16px;
      padding: 12px 16px;
      background: #fffbea;
      color: #b45309;
      border: 1px solid #fde68a;
      border-radius: 8px;
      font-size: 0.85rem;
      font-weight: 500;
    }

    @media (max-width: 900px) {
      .login-left { display: none; }
      .login-right { padding: 20px; }
    }
  `]
})
export class LoginComponent implements OnInit {
  identifier = '';
  password = '';
  errorMessage = '';
  sessionExpired = false;
  loading = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.sessionExpired = params['reason'] === 'inactivity';
    });
  }

  onLogin() {
    this.loading = true;
    this.errorMessage = '';
    const credentials = { identifier: this.identifier, password: this.password };
    
    this.authService.login(credentials).subscribe({
      next: (res) => {
        this.loading = false;
        if (res.mfaRequired) {
          this.router.navigate(['/mfa']);
        } else {
          // Check mustChangePassword after login
          this.authService.getCurrentUser().subscribe({
            next: (user) => {
              if (user.mustChangePassword) {
                this.router.navigate(['/profile/change-password']);
              } else {
                this.router.navigate(['/dashboard']);
              }
            },
            error: () => this.router.navigate(['/dashboard'])
          });
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || err.message || 'Invalid credentials. Please try again.';
      }
    });
  }
}