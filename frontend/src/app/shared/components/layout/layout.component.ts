import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { HeaderComponent } from '../header/header.component';
import { FooterComponent } from '../footer/footer.component';
import { ToastComponent } from '../toast/toast.component';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { InactivityService } from '../../../core/services/inactivity.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, SidebarComponent, HeaderComponent, FooterComponent, ToastComponent],
  template: `
    <!-- Inactivity warning banner -->
    @if (showWarning) {
      <div class="inactivity-overlay">
        <div class="inactivity-modal">
          <div class="inactivity-icon">⏱️</div>
          <h3>Session Expiring Soon</h3>
          <p>You will be logged out due to inactivity in <strong>{{ countdown }}</strong> second{{ countdown !== 1 ? 's' : '' }}.</p>
          <button class="btn-stay" (click)="stayLoggedIn()">Stay Logged In</button>
        </div>
      </div>
    }

    <div class="app-container">
      <app-sidebar #sidebarRef></app-sidebar>
      <div class="main-wrapper" [style.margin-left.px]="getMainMargin()" [style.width]="'calc(100% - ' + getMainMargin() + 'px)'">
        <app-header [sidebarRef]="sidebarRef"></app-header>
        <main class="main-content">
          <router-outlet></router-outlet>
        </main>
        <app-footer></app-footer>
      </div>
    </div>
    <app-toast></app-toast>
  `,
  styles: [`
    .inactivity-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,.45);
      display: flex; align-items: center; justify-content: center;
      z-index: 9999;
    }
    .inactivity-modal {
      background: #fff; border-radius: 14px; padding: 36px 40px;
      text-align: center; max-width: 360px; width: 90%;
      box-shadow: 0 20px 60px rgba(0,0,0,.2);
    }
    .inactivity-icon { font-size: 2.5rem; margin-bottom: 10px; }
    .inactivity-modal h3 { margin: 0 0 10px; font-size: 1.2rem; color: #111; }
    .inactivity-modal p  { color: #555; margin: 0 0 24px; font-size: .95rem; line-height: 1.5; }
    .btn-stay {
      background: #111; color: #fff; border: none;
      padding: 10px 28px; border-radius: 8px; font-size: .95rem;
      cursor: pointer; transition: background .2s;
    }
    .btn-stay:hover { background: #333; }
  `],
  styleUrls: ['./layout.component.css']
})
export class LayoutComponent implements OnInit, OnDestroy {
  @ViewChild('sidebarRef') sidebarRef!: SidebarComponent;

  showWarning = false;
  countdown   = 60;
  private subs: Subscription[] = [];

  constructor(private inactivity: InactivityService) {}

  ngOnInit(): void {
    this.inactivity.start();
    this.subs.push(
      this.inactivity.showWarning$.subscribe(v => this.showWarning = v),
      this.inactivity.countdown$.subscribe(v  => this.countdown   = v),
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    this.inactivity.stop();
  }

  stayLoggedIn(): void {
    this.inactivity.extendSession();
  }

  getMainMargin(): number {
    if (!this.sidebarRef) return 240;
    if (window.innerWidth <= 768) return 0;
    return this.sidebarRef.collapsed ? 64 : 240;
  }
}
