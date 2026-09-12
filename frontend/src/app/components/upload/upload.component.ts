import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../../api.service';
import { BatchSummary } from '../../models';
import { ToastService } from '../../toast.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './upload.component.html'
})
export class UploadComponent implements OnInit {
  selected: File | null = null;
  fileError = '';
  uploading = false;
  uploadSuccessMessage = '';
  inlineErrorMessage = '';
  batches: BatchSummary[] = [];
  loadingBatches = false;

  private api = inject(ApiService);
  private toast = inject(ToastService);
  private changeDetector = inject(ChangeDetectorRef);
  private router = inject(Router, { optional: true });

  ngOnInit() {
    this.loadBatches();
  }

  goToAlerts() {
    this.router?.navigate(['/alerts']);
  }

  loadBatches() {
    if (!this.api?.batches) return;
    this.loadingBatches = true;
    this.api.batches(0, 10).subscribe({
      next: page => {
        this.batches = page?.content || [];
        this.loadingBatches = false;
        this.changeDetector.detectChanges();
      },
      error: () => {
        this.batches = [];
        this.loadingBatches = false;
        this.changeDetector.detectChanges();
      }
    });
  }

  select(file: File | null) {
    this.selected = file;
    this.fileError = '';
    this.uploadSuccessMessage = '';
    this.inlineErrorMessage = '';
    if (!file) return;
    const validType = /\.(xlsx?|xls)$/i.test(file.name);
    if (!validType) {
      this.fileError = 'Select an XLS or XLSX file.';
    } else if (file.size > 100 * 1024 * 1024) {
      this.fileError = 'File size must be 100 MB or less.';
    }
  }

  upload(fileInput?: HTMLInputElement) {
    if (!this.selected || this.fileError || this.uploading) return;
    this.uploading = true;
    this.uploadSuccessMessage = '';
    this.inlineErrorMessage = '';
    this.changeDetector.detectChanges();

    this.api.upload(this.selected).subscribe({
      next: message => {
        this.uploading = false;
        const msg = message || 'Transactions uploaded and screened successfully.';
        this.uploadSuccessMessage = msg;
        this.toast.show(msg);
        this.selected = null;
        if (fileInput) fileInput.value = '';
        this.loadBatches();
        this.changeDetector.detectChanges();
      },
      error: error => {
        this.uploading = false;
        let errorMessage = 'Upload failed.';
        const raw = error?.error;
        if (typeof raw === 'string') {
          try {
            const parsed = JSON.parse(raw);
            errorMessage = parsed.message || raw;
          } catch {
            errorMessage = raw;
          }
        } else if (raw?.message) {
          errorMessage = raw.message;
        } else if (error?.message) {
          errorMessage = error.message;
        }

        this.inlineErrorMessage = errorMessage;
        this.toast.show(errorMessage, 'danger');
        // Clear selected file and reset file input to prevent accidental re-submission
        this.selected = null;
        if (fileInput) fileInput.value = '';
        this.changeDetector.detectChanges();
      }
    });
  }
}


