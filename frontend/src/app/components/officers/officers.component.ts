import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../api.service';
import { Alert, Officer } from '../../models';
import { ToastService } from '../../toast.service';
import { isEmail, isUsername } from '../../validation';

@Component({ selector: 'app-officers', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './officers.component.html' })
export class OfficersComponent {
  officers: Officer[] = []; assignedAlerts: Alert[] = []; assignedOfficer?: Officer; selectedAlert?: Alert; form = { fullName: '', username: '', email: '' };
  officerPage = 0; alertPage = 0; readonly pageSize = 5;
  readonly Math = Math;
  private api = inject(ApiService); private toast = inject(ToastService); private changeDetector = inject(ChangeDetectorRef);
  constructor() { this.load(); }
  get pagedOfficers() { return this.officers.slice(this.officerPage * this.pageSize, (this.officerPage + 1) * this.pageSize); }
  get officerPageCount() { return Math.max(1, Math.ceil(this.officers.length / this.pageSize)); }
  get pagedAssignedAlerts() { return this.assignedAlerts.slice(this.alertPage * this.pageSize, (this.alertPage + 1) * this.pageSize); }
  get alertPageCount() { return Math.max(1, Math.ceil(this.assignedAlerts.length / this.pageSize)); }
  load() { this.api.officers().subscribe({ next: data => { this.officers = data; this.officerPage = 0; this.changeDetector.detectChanges(); }, error: error => { this.toast.show(error.error?.message || 'Could not load officers.', 'danger'); this.changeDetector.detectChanges(); } }); }
  viewAlerts(officer: Officer) { if (!officer.userId) return; this.assignedOfficer = officer; this.selectedAlert = undefined; this.alertPage = 0; this.api.officerAlerts(officer.userId).subscribe({ next: data => { this.assignedAlerts = data; this.changeDetector.detectChanges(); }, error: error => { this.toast.show(error.error?.message || 'Could not load officer alerts.', 'danger'); this.changeDetector.detectChanges(); } }); }
  viewAlert(alert: Alert) { this.selectedAlert = alert; }
  closeAlertDrawer() { this.selectedAlert = undefined; }
  setOfficerPage(page: number) { if (page >= 0 && page < this.officerPageCount) this.officerPage = page; }
  setAlertPage(page: number) { if (page >= 0 && page < this.alertPageCount) this.alertPage = page; }
  create(formRef: any) { const fullName = this.form.fullName.trim(); const username = this.form.username.trim(); const email = this.form.email.trim(); if (formRef.invalid || fullName.length < 2 || fullName.length > 128 || !isUsername(username) || !isEmail(email)) return; const payload = { fullName, username, email }; this.api.createOfficer(payload).subscribe({ next: () => { this.toast.show('Officer created.'); this.form = { fullName: '', username: '', email: '' }; formRef.resetForm(); this.load(); }, error: error => this.toast.show(error.error?.message || 'Could not create officer.', 'danger') }); }
  toggle(officer: Officer) { if (!officer.userId) return; const request = officer.isActive === false ? this.api.reactivateOfficer(officer.userId) : this.api.deactivateOfficer(officer.userId); request.subscribe({ next: () => { this.toast.show('Officer status updated.'); this.load(); }, error: error => this.toast.show(error.error?.message || 'Could not update officer.', 'danger') }); }
}
