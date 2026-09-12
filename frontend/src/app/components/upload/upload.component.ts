import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../api.service';
import { ToastService } from '../../toast.service';

@Component({ selector:'app-upload', standalone:true, imports:[CommonModule], templateUrl:'./upload.component.html' })
export class UploadComponent { selected:File|null=null; fileError=''; uploading=false; private api=inject(ApiService); private toast=inject(ToastService); private changeDetector=inject(ChangeDetectorRef); select(file:File|null){this.selected=file;this.fileError='';if(!file)return;const validType=/\.(xlsx?|xls)$/i.test(file.name);if(!validType)this.fileError='Select an XLS or XLSX file.';else if(file.size>100*1024*1024)this.fileError='File size must be 100 MB or less.';} upload(){if(!this.selected||this.fileError)return;this.uploading=true;this.changeDetector.detectChanges();this.api.upload(this.selected).subscribe({next:m=>{this.uploading=false;this.toast.show(m||'Transactions uploaded successfully.');this.changeDetector.detectChanges();},error:e=>{this.uploading=false;this.toast.show(e.error?.message||e.error||'Upload failed.','danger');this.changeDetector.detectChanges();}});}}
