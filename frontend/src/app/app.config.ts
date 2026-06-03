import { ApplicationConfig, provideZoneChangeDetection, APP_INITIALIZER } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http'; 
import { authInterceptor } from './core/interceptors/auth-interceptor';
import { routes } from './app.routes';
import { AuthService } from './core/services/auth.service';

function initPermissions(authService: AuthService) {
  return () => {
    // If logged in, always refresh permissions from backend on app start/refresh
    if (authService.getToken()) {
      return authService.getCurrentUser().toPromise().catch(() => {});
    }
    return Promise.resolve();
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    {
      provide: APP_INITIALIZER,
      useFactory: initPermissions,
      deps: [AuthService],
      multi: true
    }
  ]
};
