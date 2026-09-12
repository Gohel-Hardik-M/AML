import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AlertsPagedComponent } from './alerts-paged.component';
import { ApiService } from '../../api.service';
import { AuthService } from '../../auth.service';
import { ToastService } from '../../toast.service';
import { Alert, PageResponse } from '../../models';

describe('AlertsPagedComponent', () => {
  let apiService: {
    alerts: ReturnType<typeof vi.fn>;
    myAlerts: ReturnType<typeof vi.fn>;
    officers: ReturnType<typeof vi.fn>;
    batches: ReturnType<typeof vi.fn>;
    myBatchIds: ReturnType<typeof vi.fn>;
    batchAlerts: ReturnType<typeof vi.fn>;
    myBatchAlerts: ReturnType<typeof vi.fn>;
    alertDetail: ReturnType<typeof vi.fn>;
    closeAlert: ReturnType<typeof vi.fn>;
    alertPdf: ReturnType<typeof vi.fn>;
    assignAlerts: ReturnType<typeof vi.fn>;
    unassignAlerts: ReturnType<typeof vi.fn>;
  };
  let authService: { currentRole: ReturnType<typeof vi.fn> };
  let toastService: { show: ReturnType<typeof vi.fn> };

  const mockPage: PageResponse<Alert> = {
    content: [
      { alertId: 'alt-1', ruleCode: 'RULE_VELOCITY', severity: 'HIGH', customerId: 'CUST100', reviewed: false },
      { alertId: 'alt-2', ruleCode: 'RULE_STRUCTURING', severity: 'CRITICAL', customerId: 'CUST200', reviewed: true },
      { alertId: 'alt-3', ruleCode: 'RULE_GEO_RISK', severity: 'MEDIUM', customerId: 'CUST300', reviewed: false },
    ],
    totalElements: 3,
    totalPages: 1,
    number: 0,
    size: 30
  };

  beforeEach(async () => {
    apiService = {
      alerts: vi.fn().mockReturnValue(of(mockPage)),
      myAlerts: vi.fn().mockReturnValue(of(mockPage)),
      officers: vi.fn().mockReturnValue(of([
        { userId: 'off-1', username: 'alice', fullName: 'Alice Smith', isActive: true },
        { userId: 'off-2', username: 'bob', fullName: 'Bob Jones', isActive: false },
      ])),
      batches: vi.fn().mockReturnValue(of({ content: [{ batchId: 'b-1', fileName: 'tx.xlsx' }] })),
      myBatchIds: vi.fn().mockReturnValue(of(['b-1'])),
      batchAlerts: vi.fn().mockReturnValue(of([])),
      myBatchAlerts: vi.fn().mockReturnValue(of([])),
      alertDetail: vi.fn(),
      closeAlert: vi.fn(),
      alertPdf: vi.fn(),
      assignAlerts: vi.fn(),
      unassignAlerts: vi.fn(),
    };
    authService = { currentRole: vi.fn().mockReturnValue('TENANT_ADMIN') };
    toastService = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [AlertsPagedComponent],
      providers: [
        { provide: ApiService, useValue: apiService },
        { provide: AuthService, useValue: authService },
        { provide: ToastService, useValue: toastService },
      ],
    }).compileComponents();
  });

  it('initializes for TENANT_ADMIN, loads alerts, filters active officers, and loads batches', () => {
    const fixture = TestBed.createComponent(AlertsPagedComponent);
    const comp = fixture.componentInstance;

    expect(apiService.alerts).toHaveBeenCalledWith(false, 0, 30);
    expect(apiService.officers).toHaveBeenCalled();
    // Only active officers should be retained
    expect(comp.officers.length).toBe(1);
    expect(comp.officers[0].userId).toBe('off-1');
    expect(comp.alerts.length).toBe(3);
    expect(comp.filtered.length).toBe(3);
  });

  it('filters alerts by query, severity, and review status', () => {
    const fixture = TestBed.createComponent(AlertsPagedComponent);
    const comp = fixture.componentInstance;

    // Filter by text query
    comp.query = 'CUST100';
    comp.filter();
    expect(comp.filtered.length).toBe(1);
    expect(comp.filtered[0].alertId).toBe('alt-1');

    // Filter by severity
    comp.query = '';
    comp.severity = 'CRITICAL';
    comp.filter();
    expect(comp.filtered.length).toBe(1);
    expect(comp.filtered[0].alertId).toBe('alt-2');

    // Filter by reviewed status
    comp.severity = '';
    comp.reviewed = 'false';
    comp.filter();
    expect(comp.filtered.length).toBe(2);
  });

  it('toggles selection of single alert and all visible alerts', () => {
    const fixture = TestBed.createComponent(AlertsPagedComponent);
    const comp = fixture.componentInstance;

    expect(comp.selectedIds.length).toBe(0);

    comp.toggleSelected('alt-1');
    expect(comp.selectedIds).toEqual(['alt-1']);
    expect(comp.allVisibleSelected).toBe(false);

    // Select all visible
    comp.toggleAllVisible();
    expect(comp.selectedIds.sort()).toEqual(['alt-1', 'alt-2', 'alt-3']);
    expect(comp.allVisibleSelected).toBe(true);

    // Deselect all visible
    comp.toggleAllVisible();
    expect(comp.selectedIds.length).toBe(0);
    expect(comp.allVisibleSelected).toBe(false);
  });

  it('assigns selected alerts to chosen officer and unassigns', () => {
    apiService.assignAlerts.mockReturnValue(of({ success: true }));
    apiService.unassignAlerts.mockReturnValue(of({ success: true }));

    const fixture = TestBed.createComponent(AlertsPagedComponent);
    const comp = fixture.componentInstance;

    comp.toggleSelected('alt-1');
    comp.toggleSelected('alt-2');
    comp.officerId = 'off-1';

    comp.assignSelected();
    expect(apiService.assignAlerts).toHaveBeenCalledWith({
      officerId: 'off-1',
      alertIds: ['alt-1', 'alt-2']
    });
    expect(toastService.show).toHaveBeenCalledWith('Alerts assigned to the selected officer.');

    // Unassign
    comp.toggleSelected('alt-1');
    comp.unassignSelected();
    expect(apiService.unassignAlerts).toHaveBeenCalledWith(['alt-1']);
    expect(toastService.show).toHaveBeenCalledWith('Alerts unassigned.');
  });

  it('closing an alert requires review notes', () => {
    apiService.closeAlert.mockReturnValue(of({}));
    const fixture = TestBed.createComponent(AlertsPagedComponent);
    const comp = fixture.componentInstance;

    comp.detail = mockPage.content[0];
    comp.reviewNotes = '';
    comp.close();
    expect(apiService.closeAlert).not.toHaveBeenCalled();

    comp.reviewNotes = 'Legitimate high-value international transaction verified.';
    comp.close();
    expect(apiService.closeAlert).toHaveBeenCalledWith('alt-1', 'Legitimate high-value international transaction verified.');
    expect(toastService.show).toHaveBeenCalledWith('Alert closed.');
    expect(comp.detail).toBeUndefined();
  });

  it('generates PDF for case file with download trigger', () => {
    const mockBlob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
    apiService.alertPdf.mockReturnValue(of(mockBlob));

    // Spy URL createObjectURL and revokeObjectURL
    const createUrlSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock-url');
    const revokeUrlSpy = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});

    const fixture = TestBed.createComponent(AlertsPagedComponent);
    const comp = fixture.componentInstance;

    comp.detail = mockPage.content[0];
    comp.reviewNotes = 'Case notes for regulatory filing.';

    comp.generatePdf();

    expect(apiService.alertPdf).toHaveBeenCalledWith('alt-1', 'Case notes for regulatory filing.');
    expect(createUrlSpy).toHaveBeenCalledWith(mockBlob);
    expect(revokeUrlSpy).toHaveBeenCalledWith('blob:mock-url');
    expect(toastService.show).toHaveBeenCalledWith('Case filed and PDF generated.');
  });

  it('determines severity CSS classes correctly using Bootstrap utilities', () => {
    const fixture = TestBed.createComponent(AlertsPagedComponent);
    const comp = fixture.componentInstance;

    expect(comp.severityClass('CRITICAL')).toBe('text-bg-danger');
    expect(comp.severityClass('HIGH')).toBe('text-bg-warning');
    expect(comp.severityClass('MEDIUM')).toBe('text-bg-info');
    expect(comp.severityClass('LOW')).toBe('text-bg-info');
  });
});
