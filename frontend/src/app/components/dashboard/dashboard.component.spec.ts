import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { ApiService } from '../../api.service';
import { AuthService } from '../../auth.service';
import { Alert, PageResponse } from '../../models';

describe('DashboardComponent', () => {
  let api: { alerts: ReturnType<typeof vi.fn>; myAlerts: ReturnType<typeof vi.fn> };
  let auth: { currentRole: ReturnType<typeof vi.fn>; tenantId: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    api = {
      alerts: vi.fn(),
      myAlerts: vi.fn(),
    };
    auth = {
      currentRole: vi.fn().mockReturnValue('TENANT_ADMIN'),
      tenantId: vi.fn().mockReturnValue('BANK_A'),
    };

    const mockAlerts: Alert[] = [
      { alertId: 'a1', ruleCode: 'R1', reviewed: false, triggeredAmount: 100 },
      { alertId: 'a2', ruleCode: 'R2', reviewed: true, triggeredAmount: 200 },
    ];
    const page: PageResponse<Alert> = {
      content: mockAlerts,
      totalElements: 2,
      totalPages: 1,
      number: 0,
      size: 10,
    };
    api.alerts.mockReturnValue(of(page));
    api.myAlerts.mockReturnValue(of(page));

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: ApiService, useValue: api },
        { provide: AuthService, useValue: auth },
      ],
    }).compileComponents();
  });

  it('loads alerts and computes statistics for TENANT_ADMIN', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;

    expect(api.alerts).toHaveBeenCalled();
    expect(component.stats[0].value).toBe('2'); // Total
    expect(component.stats[1].value).toBe('1'); // Unreviewed
    expect(component.stats[2].value).toBe('1'); // Reviewed
    expect(component.stats[3].value).toBe('TENANT_ADMIN');
    expect(component.isTenantAdmin).toBe(true);
  });

  it('calls myAlerts when current role is COMPLIANCE_OFFICER', () => {
    auth.currentRole.mockReturnValue('COMPLIANCE_OFFICER');
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;

    expect(api.myAlerts).toHaveBeenCalled();
    expect(component.roleLabel).toBe('Compliance officer workspace');
  });

  it('skips alert queries when current role is SYSTEM_ADMIN', () => {
    auth.currentRole.mockReturnValue('SYSTEM_ADMIN');
    const fixture = TestBed.createComponent(DashboardComponent);
    const component = fixture.componentInstance;

    expect(api.alerts).not.toHaveBeenCalled();
    expect(api.myAlerts).not.toHaveBeenCalled();
    expect(component.isSystemAdmin).toBe(true);
  });
});
