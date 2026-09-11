import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, finalize, throwError } from 'rxjs';
import { LoadingService } from './loading.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = sessionStorage.getItem('aml_token');
  const router = inject(Router);
  const loading = inject(LoadingService);
  const isApiRequest = request.url.startsWith('/api');
  const apiRequest = isApiRequest ? request.clone({ setHeaders: token ? { Authorization: `Bearer ${token}` } : {} }) : request;
  if (isApiRequest) loading.start();
  return next(apiRequest).pipe(catchError((error) => {
    if (isApiRequest && error.status === 401) {
      sessionStorage.clear();
      void router.navigate(['/login'], { replaceUrl: true });
    }
    return throwError(() => error);
  }), finalize(() => { if (isApiRequest) loading.stop(); }));
};
