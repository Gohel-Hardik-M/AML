import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../api.service';
import { RuleConfig } from '../../models';
import { ToastService } from '../../toast.service';
import { isJsonObject, isRuleCode } from '../../validation';

@Component({ selector: 'app-rules', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './rules.component.html', styleUrl: './rules.component.css' })
export class RulesComponent {
  rules: RuleConfig[] = [];
  selectedRule?: RuleConfig;
  private api = inject(ApiService);
  private toast = inject(ToastService);
  private changeDetector = inject(ChangeDetectorRef);
  constructor() { this.load(); }
  load() { this.api.rules().subscribe({ next: data => { this.rules = data; this.changeDetector.detectChanges(); }, error: error => { this.toast.show(error.error?.message || 'Could not load rules.', 'danger'); this.changeDetector.detectChanges(); } }); }
  loadDetail(code: string) { this.api.rule(code).subscribe({ next: data => { this.selectedRule = data; this.changeDetector.detectChanges(); }, error: error => { this.toast.show(error.error?.message || 'Could not load rule details.', 'danger'); this.changeDetector.detectChanges(); } }); }
  valid(rule: RuleConfig) { return isRuleCode(rule.ruleCode) && (rule.thresholdAmount == null || rule.thresholdAmount >= 0.01) && (rule.windowMinutes == null || Number.isInteger(rule.windowMinutes) && rule.windowMinutes > 0) && (rule.maxCount == null || Number.isInteger(rule.maxCount) && rule.maxCount > 0) && (rule.percentageDeviation == null || (rule.percentageDeviation >= 0 && rule.percentageDeviation <= 100)) && isJsonObject(rule.customParametersJson || ''); }
  save(rule: RuleConfig) { if (!rule.ruleCode.trim() || !this.valid(rule)) return; const payload = { thresholdAmount: rule.thresholdAmount, windowMinutes: rule.windowMinutes, maxCount: rule.maxCount, isEnabled: rule.isEnabled, percentageDeviation: rule.percentageDeviation, customParametersJson: rule.customParametersJson }; this.api.updateRule(rule.ruleCode, payload).subscribe({ next: () => this.toast.show(`${rule.ruleCode} updated.`), error: error => this.toast.show(error.error?.message || 'Could not update rule.', 'danger') }); }
}
