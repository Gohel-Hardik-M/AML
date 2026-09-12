import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../api.service';
import { Alert, BatchSummary, Officer, PageResponse } from '../../models';
import { AuthService } from '../../auth.service';
import { ToastService } from '../../toast.service';

@Component({
  selector: 'app-alerts-paged',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './alerts-paged.component.html',
})
export class AlertsPagedComponent {
  alerts: Alert[] = [];
  filtered: Alert[] = [];
  batches: BatchSummary[] = [];
  detail?: Alert;
  query = '';
  severity = '';
  reviewed = '';
  officerId = '';
  reviewNotes = '';
  selected: Record<string, boolean> = {};
  officers: Officer[] = [];
  loading = false;
  page: PageResponse<Alert> = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 };
  readonly pageSize = 30;
  readonly Math = Math;
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private toast = inject(ToastService);
  private changeDetector = inject(ChangeDetectorRef);

  get isCompliance() { return this.auth.currentRole() === 'COMPLIANCE_OFFICER'; }
  get isTenantAdmin() { return this.auth.currentRole() === 'TENANT_ADMIN'; }
  get selectedIds() { return Object.keys(this.selected).filter(id => this.selected[id]); }
  get allVisibleSelected() { return this.filtered.length > 0 && this.filtered.every(alert => !!alert.alertId && this.selected[alert.alertId]); }

  constructor() {
    this.loadPage(0);
    if (this.isTenantAdmin) this.api.officers().subscribe({ next: officers => { this.officers = officers.filter(officer => officer.isActive !== false); this.changeDetector.detectChanges(); }, error: error => this.toast.show(error.error?.message || 'Could not load compliance officers.', 'danger') });
    if (this.isCompliance) this.api.myBatchIds().subscribe({ next: ids => { this.batches = ids.map(batchId => ({ batchId, fileName: 'Assigned alert batch', status: 'ASSIGNED' })); this.changeDetector.detectChanges(); }, error: error => this.toast.show(error.error?.message || 'Could not load assigned batches.', 'danger') });
    else this.api.batches().subscribe({ next: data => { this.batches = data.content; this.changeDetector.detectChanges(); }, error: error => this.toast.show(error.error?.message || 'Could not load batches.', 'danger') });
  }

  loadPage(page: number) {
    if (page < 0 || this.loading) return;
    this.loading = true;
    const request = this.isCompliance ? this.api.myAlerts(page, this.pageSize) : this.api.alerts(false, page, this.pageSize);
    request.subscribe({ next: result => { this.page = result; this.alerts = result.content; this.selected = {}; this.filter(); this.loading = false; this.changeDetector.detectChanges(); }, error: error => { this.loading = false; this.toast.show(error.error?.message || 'Could not load alerts.', 'danger'); this.changeDetector.detectChanges(); } });
  }

  filter() {
    const query = this.query.trim().toLowerCase();
    this.filtered = this.alerts.filter(alert => (!query || JSON.stringify(alert).toLowerCase().includes(query)) && (!this.severity || alert.severity?.toUpperCase() === this.severity) && (!this.reviewed || String(!!alert.reviewed) === this.reviewed));
  }

  details(alert: Alert) {
    if (!alert.alertId) return;
    if (this.isCompliance) this.api.alertDetail(alert.alertId).subscribe({ next: data => { this.detail = data; this.reviewNotes = ''; this.changeDetector.detectChanges(); }, error: error => this.toast.show(error.error?.message || 'Could not load alert detail.', 'danger') });
    else this.detail = alert;
  }

  close() {
    if (!this.detail?.alertId) return;
    if (!this.reviewNotes.trim()) return;
    this.api.closeAlert(this.detail.alertId, this.reviewNotes.trim()).subscribe({ next: () => { this.detail = undefined; this.reviewNotes = ''; this.toast.show('Alert closed.'); this.loadPage(this.page.number); }, error: error => this.toast.show(error.error?.message || 'Could not close alert.', 'danger') });
  }

  generatePdf() {
    if (!this.detail?.alertId) return;
    if (!this.reviewNotes.trim()) return;
    this.api.alertPdf(this.detail.alertId, this.reviewNotes.trim()).subscribe({ next: file => { const alertId = this.detail?.alertId; const url = URL.createObjectURL(file); const link = document.createElement('a'); link.href = url; link.download = `aml-alert-${alertId}.pdf`; link.click(); URL.revokeObjectURL(url); this.detail = undefined; this.reviewNotes = ''; this.toast.show('Case filed and PDF generated.'); this.loadPage(this.page.number); }, error: error => this.toast.show(error.error?.message || 'Could not generate alert PDF.', 'danger') });
  }

  clearDetail() { this.detail = undefined; }
  toggleSelected(alertId: string | undefined) { if (!alertId) return; this.selected = { ...this.selected, [alertId]: !this.selected[alertId] }; }
  toggleAllVisible() { const select = !this.allVisibleSelected; const next = { ...this.selected }; this.filtered.forEach(alert => { if (alert.alertId) next[alert.alertId] = select; }); this.selected = next; }
  assignSelected() { if (!this.officerId || !this.selectedIds.length) return; this.api.assignAlerts({ officerId: this.officerId, alertIds: this.selectedIds }).subscribe({ next: () => { this.toast.show('Alerts assigned to the selected officer.'); this.selected = {}; this.loadPage(this.page.number); }, error: error => this.toast.show(error.error?.message || 'Could not assign alerts.', 'danger') }); }
  unassignSelected() { if (!this.selectedIds.length) return; this.api.unassignAlerts(this.selectedIds).subscribe({ next: () => { this.toast.show('Alerts unassigned.'); this.selected = {}; this.loadPage(this.page.number); }, error: error => this.toast.show(error.error?.message || 'Could not unassign alerts.', 'danger') }); }
  loadBatch(batchId: string) { const request = this.isCompliance ? this.api.myBatchAlerts(batchId) : this.api.batchAlerts(batchId); request.subscribe({ next: data => { this.alerts = data; this.page = { content: data, totalElements: data.length, totalPages: 1, number: 0, size: data.length }; this.filter(); }, error: error => this.toast.show(error.error?.message || 'Could not load batch alerts.', 'danger') }); }
  severityClass(value?: string) { return value?.toUpperCase() === 'CRITICAL' ? 'text-bg-danger' : value?.toUpperCase() === 'HIGH' ? 'text-bg-warning' : 'text-bg-info'; }
}
