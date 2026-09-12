import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../../api.service';
import { ToastService } from '../../toast.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './upload.component.html'
})
export class UploadComponent {
  selected: File | null = null;
  fileError = '';
  uploading = false;
  uploadSuccessMessage = '';

  private api = inject(ApiService);
  private toast = inject(ToastService);
  private changeDetector = inject(ChangeDetectorRef);
  private router = inject(Router, { optional: true });

  goToAlerts() {
    this.router?.navigate(['/alerts']);
  }

  select(file: File | null) {
    this.selected = file;
    this.fileError = '';
    this.uploadSuccessMessage = '';
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
    this.changeDetector.detectChanges();

    this.api.upload(this.selected).subscribe({
      next: message => {
        this.uploading = false;
        const msg = message || 'Transactions uploaded successfully.';
        this.uploadSuccessMessage = msg;
        this.toast.show(msg);
        this.selected = null;
        if (fileInput) fileInput.value = '';
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

        this.toast.show(errorMessage, 'danger');
        this.changeDetector.detectChanges();
      }
    });
  }
}

