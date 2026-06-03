import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, fromEvent, merge, Subscription } from 'rxjs';

const TIMEOUT_MS  = 10 * 60 * 1000; // 10 minutes
const WARNING_MS  =  9 * 60 * 1000; //  9 minutes — show warning 1 min before logout

@Injectable({ providedIn: 'root' })
export class InactivityService implements OnDestroy {

  /** Emits true when the 1-minute warning should be shown */
  readonly showWarning$ = new BehaviorSubject<boolean>(false);
  /** Countdown seconds shown inside the warning banner */
  readonly countdown$   = new BehaviorSubject<number>(60);

  private timeoutHandle?: ReturnType<typeof setTimeout>;
  private warningHandle?: ReturnType<typeof setTimeout>;
  private countdownInterval?: ReturnType<typeof setInterval>;
  private activitySub?: Subscription;
  private active = false;

  constructor(private router: Router, private zone: NgZone) {}

  /** Call once after successful login */
  start(): void {
    if (this.active) return;
    this.active = true;

    // Listen for any user interaction
    const events$ = merge(
      fromEvent(document, 'mousemove'),
      fromEvent(document, 'mousedown'),
      fromEvent(document, 'keydown'),
      fromEvent(document, 'scroll'),
      fromEvent(document, 'touchstart'),
      fromEvent(document, 'click'),
    );

    this.activitySub = events$.subscribe(() => this.resetTimer());
    this.scheduleTimers();
  }

  /** Call on logout / stop tracking */
  stop(): void {
    this.active = false;
    this.clearAll();
    this.showWarning$.next(false);
  }

  /** User clicked "Stay logged in" in the warning banner */
  extendSession(): void {
    this.showWarning$.next(false);
    this.resetTimer();
  }

  private resetTimer(): void {
    if (!this.active) return;
    this.clearAll();
    this.showWarning$.next(false);
    this.scheduleTimers();
  }

  private scheduleTimers(): void {
    this.zone.runOutsideAngular(() => {
      // Show warning at 9 minutes
      this.warningHandle = setTimeout(() => {
        this.zone.run(() => {
          this.showWarning$.next(true);
          this.countdown$.next(60);
          let secs = 60;
          this.countdownInterval = setInterval(() => {
            secs--;
            this.countdown$.next(secs);
          }, 1000);
        });
      }, WARNING_MS);

      // Auto-logout at 10 minutes
      this.timeoutHandle = setTimeout(() => {
        this.zone.run(() => this.logout());
      }, TIMEOUT_MS);
    });
  }

  private logout(): void {
    this.stop();
    localStorage.clear();
    sessionStorage.clear();
    this.router.navigate(['/login'], {
      queryParams: { reason: 'inactivity' }
    });
  }

  private clearAll(): void {
    clearTimeout(this.timeoutHandle);
    clearTimeout(this.warningHandle);
    clearInterval(this.countdownInterval);
  }

  ngOnDestroy(): void {
    this.stop();
    this.activitySub?.unsubscribe();
  }
}
