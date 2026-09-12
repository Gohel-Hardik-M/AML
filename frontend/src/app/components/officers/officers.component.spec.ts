import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { OfficersComponent } from './officers.component';
import { ApiService } from '../../api.service';
import { ToastService } from '../../toast.service';
import { Officer, Alert } from '../../models';

describe('OfficersComponent', () => {
  let apiService: {
    officers: ReturnType<typeof vi.fn>;
    officerAlerts: ReturnType<typeof vi.fn>;
    createOfficer: ReturnType<typeof vi.fn>;
    deactivateOfficer: ReturnType<typeof vi.fn>;
    reactivateOfficer: ReturnType<typeof vi.fn>;
  };
  let toastService: { show: ReturnType<typeof vi.fn> };

  const mockOfficers: Officer[] = [
    { userId: 'u1', username: 'officer1', fullName: 'Officer One', email: 'o1@bank.com', isActive: true },
    { userId: 'u2', username: 'officer2', fullName: 'Officer Two', email: 'o2@bank.com', isActive: false },
    { userId: 'u3', username: 'officer3', fullName: 'Officer Three', email: 'o3@bank.com', isActive: true },
    { userId: 'u4', username: 'officer4', fullName: 'Officer Four', email: 'o4@bank.com', isActive: true },
    { userId: 'u5', username: 'officer5', fullName: 'Officer Five', email: 'o5@bank.com', isActive: true },
    { userId: 'u6', username: 'officer6', fullName: 'Officer Six', email: 'o6@bank.com', isActive: true },
  ];

  beforeEach(async () => {
    apiService = {
      officers: vi.fn().mockReturnValue(of(mockOfficers)),
      officerAlerts: vi.fn().mockReturnValue(of([])),
      createOfficer: vi.fn(),
      deactivateOfficer: vi.fn(),
      reactivateOfficer: vi.fn(),
    };
    toastService = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [OfficersComponent],
      providers: [
        { provide: ApiService, useValue: apiService },
        { provide: ToastService, useValue: toastService },
      ],
    }).compileComponents();
  });

  it('loads officers on initialization and manages pagination', () => {
    const fixture = TestBed.createComponent(OfficersComponent);
    const comp = fixture.componentInstance;

    expect(apiService.officers).toHaveBeenCalled();
    expect(comp.officers.length).toBe(6);
    expect(comp.officerPageCount).toBe(2);
    expect(comp.pagedOfficers.length).toBe(5);
    expect(comp.pagedOfficers[0].username).toBe('officer1');

    comp.setOfficerPage(1);
    expect(comp.officerPage).toBe(1);
    expect(comp.pagedOfficers.length).toBe(1);
    expect(comp.pagedOfficers[0].username).toBe('officer6');

    // Bounds check
    comp.setOfficerPage(-1);
    expect(comp.officerPage).toBe(1);
    comp.setOfficerPage(5);
    expect(comp.officerPage).toBe(1);
  });

  it('handles load error with danger toast', () => {
    apiService.officers.mockReturnValue(throwError(() => ({ error: { message: 'Failed to fetch officers' } })));
    const fixture = TestBed.createComponent(OfficersComponent);
    const comp = fixture.componentInstance;

    expect(toastService.show).toHaveBeenCalledWith('Failed to fetch officers', 'danger');
  });

  it('views officer alerts and switches pages', () => {
    const alerts: Alert[] = [
      { alertId: 'a1', ruleCode: 'RULE1', severity: 'HIGH' },
      { alertId: 'a2', ruleCode: 'RULE2', severity: 'LOW' }
    ];
    apiService.officerAlerts.mockReturnValue(of(alerts));

    const fixture = TestBed.createComponent(OfficersComponent);
    const comp = fixture.componentInstance;

    comp.viewAlerts(mockOfficers[0]);
    expect(comp.assignedOfficer).toBe(mockOfficers[0]);
    expect(apiService.officerAlerts).toHaveBeenCalledWith('u1');
    expect(comp.assignedAlerts.length).toBe(2);
    expect(comp.alertPageCount).toBe(1);

    comp.viewAlert(alerts[0]);
    expect(comp.selectedAlert).toBe(alerts[0]);

    comp.closeAlertDrawer();
    expect(comp.selectedAlert).toBeUndefined();
  });

  it('validates officer creation form rejecting invalid inputs', () => {
    const fixture = TestBed.createComponent(OfficersComponent);
    const comp = fixture.componentInstance;

    const dummyFormRef = { invalid: false, resetForm: vi.fn() };

    // Invalid full name (too short)
    comp.form = { fullName: 'A', username: 'valid_user', email: 'valid@example.com' };
    comp.create(dummyFormRef);
    expect(apiService.createOfficer).not.toHaveBeenCalled();

    // Invalid username (spaces/symbols)
    comp.form = { fullName: 'Valid Name', username: 'inv@lid name', email: 'valid@example.com' };
    comp.create(dummyFormRef);
    expect(apiService.createOfficer).not.toHaveBeenCalled();

    // Invalid email
    comp.form = { fullName: 'Valid Name', username: 'valid_user', email: 'not-an-email' };
    comp.create(dummyFormRef);
    expect(apiService.createOfficer).not.toHaveBeenCalled();
  });

  it('creates officer successfully when input is valid and resets form', () => {
    apiService.createOfficer.mockReturnValue(of({ status: 'SUCCESS' }));
    const fixture = TestBed.createComponent(OfficersComponent);
    const comp = fixture.componentInstance;

    const dummyFormRef = { invalid: false, resetForm: vi.fn() };
    comp.form = { fullName: 'Sarah Connor', username: 'sconnor', email: 'sconnor@bank.com' };

    comp.create(dummyFormRef);
    expect(apiService.createOfficer).toHaveBeenCalledWith({
      fullName: 'Sarah Connor',
      username: 'sconnor',
      email: 'sconnor@bank.com'
    });
    expect(toastService.show).toHaveBeenCalledWith('Officer created.');
    expect(dummyFormRef.resetForm).toHaveBeenCalled();
  });

  it('toggles officer active status correctly (deactivate vs reactivate)', () => {
    apiService.deactivateOfficer.mockReturnValue(of({}));
    apiService.reactivateOfficer.mockReturnValue(of({}));

    const fixture = TestBed.createComponent(OfficersComponent);
    const comp = fixture.componentInstance;

    // Toggle active officer -> deactivates
    comp.toggle(mockOfficers[0]);
    expect(apiService.deactivateOfficer).toHaveBeenCalledWith('u1');
    expect(toastService.show).toHaveBeenCalledWith('Officer status updated.');

    // Toggle inactive officer -> reactivates
    comp.toggle(mockOfficers[1]);
    expect(apiService.reactivateOfficer).toHaveBeenCalledWith('u2');
  });
});
