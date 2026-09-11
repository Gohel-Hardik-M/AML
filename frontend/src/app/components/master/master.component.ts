import { ChangeDetectorRef, Component, inject, NgZone, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../api.service';
import { RuleCatalogItem, TenantAllocation, TenantRuleAllocationSummary, TenantSummary } from '../../models';
import { ToastService } from '../../toast.service';
import { isEmail, isRuleCode, isTenantId, isUsername } from '../../validation';

@Component({ selector: 'app-master', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './master.component.html', styleUrl: './master.component.css' })
export class MasterComponent implements OnInit {
  tenant = { tenantCode: '', bankName: '', adminUsername: '', adminEmail: '' };
  allocationTenant = ''; lookupTenant = ''; ruleSearch = '';
  catalog: RuleCatalogItem[] = []; tenants: TenantSummary[] = []; allocation: TenantAllocation[] = []; coverage: TenantRuleAllocationSummary[] = [];
  selectedRuleCodes: string[] = []; allocationLoaded = false; loadingAllocation = false; removingRule = '';
  tenantsLoading = false; tenantLoadError = ''; allocationLoadError = ''; catalogLoading = false; catalogLoadError = ''; coverageLoading = false; coverageLoadError = '';
  private api = inject(ApiService); private toast = inject(ToastService); private changeDetector = inject(ChangeDetectorRef); private zone = inject(NgZone);
  ngOnInit() { this.loadCatalog(); this.loadTenants(); this.loadCoverage(); }
  get groupedCoverage() { const groups = new Map<string, { tenantId: string; bankName: string; isActive: boolean; rules: string[] }>(); for (const item of this.coverage) { const current = groups.get(item.tenantId) || { tenantId: item.tenantId, bankName: item.bankName, isActive: item.isActive !== false, rules: [] }; if (item.ruleCode) current.rules.push(item.ruleCode); groups.set(item.tenantId, current); } return [...groups.values()]; }
  get filteredCatalog() { const search = this.ruleSearch.trim().toLowerCase(); return this.catalog.filter(rule => !search || `${rule.ruleCode} ${rule.description || ''}`.toLowerCase().includes(search)); }
  loadCatalog() { this.catalogLoading = true; this.api.ruleCatalog().subscribe({ next: data => { this.catalog = data; this.catalogLoading = false; }, error: error => { this.catalogLoading = false; this.catalogLoadError = error.error?.message || 'Rule catalog unavailable.'; this.toast.show(this.catalogLoadError, 'danger'); } }); }
  loadTenants() { this.tenantsLoading = true; this.api.tenantsList().subscribe({ next: data => { this.tenants = data; this.tenantsLoading = false; this.changeDetector.detectChanges(); }, error: error => { this.tenantsLoading = false; this.tenantLoadError = error.error?.message || 'Tenant API unavailable.'; this.toast.show(this.tenantLoadError, 'danger'); } }); }
  loadCoverage() { this.coverageLoading = true; this.api.allTenantAllocations().subscribe({ next: data => { this.coverage = data; this.coverageLoading = false; }, error: error => { this.coverageLoading = false; this.coverageLoadError = error.error?.message || 'Rule coverage unavailable.'; } }); }
  chooseTenant(tenantId: string) { this.allocationTenant = tenantId; this.lookupTenant = tenantId; this.allocation = []; this.allocationLoaded = false; document.getElementById('allocation-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' }); }
  isSelected(ruleCode: string) { return this.selectedRuleCodes.includes(ruleCode); }
  toggleRule(ruleCode: string) { this.selectedRuleCodes = this.isSelected(ruleCode) ? this.selectedRuleCodes.filter(code => code !== ruleCode) : [...this.selectedRuleCodes, ruleCode]; }
  selectAll() { this.selectedRuleCodes = [...new Set([...this.selectedRuleCodes, ...this.filteredCatalog.map(rule => rule.ruleCode)])]; }
  clearSelection() { this.selectedRuleCodes = []; }
  isTenantId(value: string) { return isTenantId(value); }
  validTenant() { return isTenantId(this.tenant.tenantCode) && this.tenant.bankName.trim().length >= 2 && isUsername(this.tenant.adminUsername) && isEmail(this.tenant.adminEmail); }
  onboard(form: any) { if (form.invalid || !this.validTenant()) return; this.api.tenants({ ...this.tenant, tenantCode: this.tenant.tenantCode.trim(), bankName: this.tenant.bankName.trim(), adminUsername: this.tenant.adminUsername.trim(), adminEmail: this.tenant.adminEmail.trim() }).subscribe({ next: () => { this.toast.show('Tenant onboarded.'); form.resetForm(); this.loadTenants(); }, error: error => this.toast.show(error.error?.message || 'Could not onboard tenant.', 'danger') }); }
  allocate(form: any) { if (form.invalid || !isTenantId(this.allocationTenant) || !this.selectedRuleCodes.length || this.selectedRuleCodes.some(code => !isRuleCode(code))) return; this.api.allocateRules({ tenantId: this.allocationTenant.trim(), ruleCodes: this.selectedRuleCodes }).subscribe({ next: () => { this.toast.show('Selected rules allocated.'); this.lookupTenant = this.allocationTenant.trim(); this.loadAllocation(); this.loadCoverage(); }, error: error => this.toast.show(error.error?.message || 'Could not allocate rules.', 'danger') }); }
  loadAllocation() { const tenantId = this.lookupTenant.trim(); if (!isTenantId(tenantId) || this.loadingAllocation) return; this.loadingAllocation = true; this.api.tenantAllocation(tenantId).subscribe({ next: data => this.zone.run(() => { this.allocation = data; this.allocationLoaded = true; this.loadingAllocation = false; }), error: error => this.zone.run(() => { this.allocation = []; this.allocationLoaded = false; this.loadingAllocation = false; this.allocationLoadError = error.error?.message || `Could not load allocation (${error.status || 'network error'}).`; }) }); }
  remove(ruleCode: string) { const cleanRuleCode = ruleCode.trim(); const tenantId = this.lookupTenant.trim(); if (!cleanRuleCode || !isTenantId(tenantId) || this.removingRule) return; this.removingRule = cleanRuleCode; this.api.removeTenantRule(tenantId, cleanRuleCode).subscribe({ next: () => { this.allocation = this.allocation.filter(rule => (rule.ruleCode || '').trim() !== cleanRuleCode); this.removingRule = ''; this.toast.show('Rule removed.'); this.loadCoverage(); }, error: error => { this.removingRule = ''; this.toast.show(error.error?.message || 'Could not remove rule.', 'danger'); } }); }
}