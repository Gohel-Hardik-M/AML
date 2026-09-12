import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ApiService } from '../../api.service';
import { ToastService } from '../../toast.service';
import { UploadComponent } from './upload.component';

describe('UploadComponent', () => {
  let api: { upload: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    api = { upload: vi.fn() };
    toast = { show: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [UploadComponent],
      providers: [
        { provide: ApiService, useValue: api },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();
  });

  it('rejects unsupported extensions before making an API call', () => {
    const component = TestBed.createComponent(UploadComponent).componentInstance;

    component.select(new File(['data'], 'transactions.csv'));

    expect(component.fileError).toBe('Select an XLS or XLSX file.');
    component.upload();
    expect(api.upload).not.toHaveBeenCalled();
  });

  it('rejects files over 100 MB before making an API call', () => {
    const component = TestBed.createComponent(UploadComponent).componentInstance;
    const file = new File(['data'], 'transactions.xlsx');
    Object.defineProperty(file, 'size', { value: 100 * 1024 * 1024 + 1 });

    component.select(file);

    expect(component.fileError).toBe('File size must be 100 MB or less.');
    component.upload();
    expect(api.upload).not.toHaveBeenCalled();
  });

  it('shows success and clears uploading state after a successful upload', () => {
    api.upload.mockReturnValue(of('Batch ID: 123'));
    const component = TestBed.createComponent(UploadComponent).componentInstance;
    component.select(new File(['data'], 'transactions.xlsx'));

    component.upload();

    expect(api.upload).toHaveBeenCalledOnce();
    expect(component.uploading).toBe(false);
    expect(toast.show).toHaveBeenCalledWith('Batch ID: 123');
  });

  it('shows the backend duplicate or validation message and clears uploading state', () => {
    api.upload.mockReturnValue(throwError(() => ({ error: { message: 'A batch for this tenant and date already exists.' } })));
    const component = TestBed.createComponent(UploadComponent).componentInstance;
    component.select(new File(['data'], 'transactions.xlsx'));

    component.upload();

    expect(component.uploading).toBe(false);
    expect(toast.show).toHaveBeenCalledWith('A batch for this tenant and date already exists.', 'danger');
  });
});
