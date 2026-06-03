import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  if (req.url.includes('/auth/login') || req.url.includes('/auth/register')) {
    return next(req);
  }

  const token = localStorage.getItem('token');

  let request = req;
  if (token) {
    request = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(request).pipe(
    catchError((error) => {
      if (error.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};