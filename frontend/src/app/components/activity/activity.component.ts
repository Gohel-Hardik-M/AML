import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../api.service';
import { Activity, PageResponse } from '../../models';
import { ToastService } from '../../toast.service';

@Component({ selector: 'app-activity', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './activity.component.html' })
export class ActivityComponent {
  page: PageResponse<Activity> = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 15 };
  query = '';
  loading = false;
  readonly Math = Math;
  private api = inject(ApiService); private toast = inject(ToastService); private changeDetector = inject(ChangeDetectorRef);
  constructor() { this.load(0); }
  get filtered() { const query = this.query.trim().toLowerCase(); return this.page.content.filter(item => !query || JSON.stringify(item).toLowerCase().includes(query)); }
  load(page: number) { if (page < 0 || this.loading) return; this.loading = true; this.api.activity(page).subscribe({ next: result => { this.page = result; this.loading = false; this.changeDetector.detectChanges(); }, error: error => { this.loading = false; this.toast.show(error.error?.message || 'Could not load bank activity.', 'danger'); this.changeDetector.detectChanges(); } }); }
}
