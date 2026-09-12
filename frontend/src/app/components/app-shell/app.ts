import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../auth.service';
import { ToastService } from '../../toast.service';
import { LoadingService } from '../../loading.service';

@Component({
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  templateUrl: './app.html',
})
export class App {
  auth = inject(AuthService);
  toast = inject(ToastService);
  loading = inject(LoadingService);
  private router = inject(Router);
  logout() { this.auth.logout(); }
  isLogin() { return this.router.url === '/login'; }
}
