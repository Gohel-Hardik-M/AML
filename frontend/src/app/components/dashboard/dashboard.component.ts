import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../api.service';
import { AuthService } from '../../auth.service';
import { Alert } from '../../models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent {
  alerts: Alert[] = [];
  stats = [
    { label: 'Alerts returned', value: '—', note: 'Current endpoint result' },
    { label: 'Unreviewed', value: '—', note: 'Requires review' },
    { label: 'Reviewed', value: '—', note: 'Completed review' },
    { label: 'Role', value: '—', note: '' }
  ];
  auth = inject(AuthService);
  private api = inject(ApiService);
  private changeDetector = inject(ChangeDetectorRef);

  get isTenantAdmin() { return this.auth.currentRole() === 'TENANT_ADMIN'; }
  get isSystemAdmin() { return this.auth.currentRole() === 'SYSTEM_ADMIN'; }
  get roleLabel() { return this.auth.currentRole() === 'COMPLIANCE_OFFICER' ? 'Compliance officer workspace' : this.isSystemAdmin ? 'System administrator workspace' : 'Tenant administrator workspace'; }

  constructor() {
    const role = this.auth.currentRole();
    this.stats[3].value = role || '—';
    if (role === 'SYSTEM_ADMIN') return;
    const request = role === 'COMPLIANCE_OFFICER' ? this.api.myAlerts() : this.api.alerts();
    request.subscribe({ next: page => { this.alerts = page.content; this.stats[0].value = String(page.totalElements); this.stats[1].value = String(page.content.filter(alert => !alert.reviewed).length); this.stats[2].value = String(page.content.filter(alert => alert.reviewed).length); this.changeDetector.detectChanges(); }, error: () => this.changeDetector.detectChanges() });
  }
}
