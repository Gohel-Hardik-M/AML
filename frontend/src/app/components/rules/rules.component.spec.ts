import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { RulesComponent } from './rules.component';
import { ApiService } from '../../api.service';
import { ToastService } from '../../toast.service';
import { RuleConfig } from '../../models';

describe('RulesComponent', () => {
  let api: { rules: ReturnType<typeof vi.fn>; rule: ReturnType<typeof vi.fn>; updateRule: ReturnType<typeof vi.fn> };
  let toast: { show: ReturnType<typeof vi.fn> };

  const mockRule: RuleConfig = {
    ruleCode: 'VELOCITY_001',
    thresholdAmount: 1000,
    windowMinutes: 60,
    maxCount: 5,
    percentageDeviation: 10,
    isEnabled: true,
  };

  beforeEach(async () => {
    api = {
      rules: vi.fn().mockReturnValue(of([mockRule])),
      rule: vi.fn().mockReturnValue(of(mockRule)),
      updateRule: vi.fn().mockReturnValue(of(mockRule)),
    };
    toast = { show: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [RulesComponent],
      providers: [
        { provide: ApiService, useValue: api },
        { provide: ToastService, useValue: toast },
      ],
    }).compileComponents();
  });

  it('loads rule configurations on initialization', () => {
    const component = TestBed.createComponent(RulesComponent).componentInstance;
    expect(api.rules).toHaveBeenCalled();
    expect(component.rules.length).toBe(1);
    expect(component.rules[0].ruleCode).toBe('VELOCITY_001');
  });

  it('loads rule detail on demand', () => {
    const component = TestBed.createComponent(RulesComponent).componentInstance;
    component.loadDetail('VELOCITY_001');
    expect(api.rule).toHaveBeenCalledWith('VELOCITY_001');
    expect(component.selectedRule).toEqual(mockRule);
  });

  it('validates rule thresholds, window, and percentage deviation', () => {
    const component = TestBed.createComponent(RulesComponent).componentInstance;

    // Valid rule
    expect(component.valid(mockRule)).toBe(true);

    // Invalid threshold (< 0.01)
    const invalidThreshold = { ...mockRule, thresholdAmount: 0 };
    expect(component.valid(invalidThreshold)).toBe(false);

    // Invalid window (0)
    const invalidWindow = { ...mockRule, windowMinutes: 0 };
    expect(component.valid(invalidWindow)).toBe(false);

    // Invalid deviation (> 100)
    const invalidDeviation = { ...mockRule, percentageDeviation: 150 };
    expect(component.valid(invalidDeviation)).toBe(false);
  });

  it('saves valid rule updates and displays success toast', () => {
    const component = TestBed.createComponent(RulesComponent).componentInstance;
    component.save(mockRule);

    expect(api.updateRule).toHaveBeenCalledWith('VELOCITY_001', expect.objectContaining({
      thresholdAmount: 1000,
      windowMinutes: 60,
    }));
    expect(toast.show).toHaveBeenCalledWith('VELOCITY_001 updated.');
  });

  it('shows error toast when saving rule fails', () => {
    api.updateRule.mockReturnValue(throwError(() => ({ error: { message: 'Failed to update rule' } })));
    const component = TestBed.createComponent(RulesComponent).componentInstance;
    component.save(mockRule);

    expect(toast.show).toHaveBeenCalledWith('Failed to update rule', 'danger');
  });
});
