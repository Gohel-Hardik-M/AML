import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ApiService } from '../../api.service';
import { ToastService } from '../../toast.service';
import { MasterComponent } from './master.component';

describe('MasterComponent', () => {
  let api: {
    ruleCatalog: ReturnType<typeof vi.fn>;
    tenantsList: ReturnType<typeof vi.fn>;
    allTenantAllocations: ReturnType<typeof vi.fn>;
    allocateRules: ReturnType<typeof vi.fn>;
    tenantAllocation: ReturnType<typeof vi.fn>;
    removeTenantRule: ReturnType<typeof vi.fn>;
  };
  let toast: { show: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    api = {
      ruleCatalog: vi.fn().mockReturnValue(of([])),
      tenantsList: vi.fn().mockReturnValue(of([])),
      allTenantAllocations: vi.fn().mockReturnValue(of([])),
      allocateRules: vi.fn(),
      tenantAllocation: vi.fn(),
      removeTenantRule: vi.fn(),
    };
    toast = { show: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [MasterComponent],
      providers: [
        { provide: ApiService, useValue: api },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();
  });

  it('blocks allocating a rule already allocated to the selected tenant', () => {
    const component = TestBed.createComponent(MasterComponent).componentInstance;
    component.allocationTenant = 'BANK_A';
    component.selectedRuleCodes = ['STRUCTURING_001'];
    component.coverage = [{ tenantId: 'BANK_A', bankName: 'Bank A', isActive: true, ruleCode: 'STRUCTURING_001', ruleName: 'Structuring' }];

    component.allocate({ invalid: false });

    expect(api.allocateRules).not.toHaveBeenCalled();
    expect(toast.show).toHaveBeenCalledWith('STRUCTURING_001 is already allocated to BANK_A.', 'danger');
  });

  it('sends only validated, newly selected rules for allocation', () => {
    const component = TestBed.createComponent(MasterComponent).componentInstance;
    component.allocationTenant = 'BANK_A';
    component.selectedRuleCodes = ['VELOCITY_001'];
    api.allocateRules.mockReturnValue(of({}));
    api.tenantAllocation.mockReturnValue(of([]));

    component.allocate({ invalid: false });

    expect(api.allocateRules).toHaveBeenCalledWith({ tenantId: 'BANK_A', ruleCodes: ['VELOCITY_001'] });
  });
});
