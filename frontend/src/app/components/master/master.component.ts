import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../api.service';
import { RuleCatalogItem, TenantAllocation, TenantRuleAllocationSummary, TenantSummary } from '../../models';
import { ToastService } from '../../toast.service';
import { isEmail, isRuleCode, isTenantId, isUsername } from '../../validation';

@Component({ selector: 'app-master', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './master.component.html' })
export class MasterComponent implements OnInit {
  tenant = { tenantCode: '', bankName: '', adminUsername: '', adminEmail: '' };
  allocationTenant = '';
  lookupTenant = '';
  ruleSearch = '';
  catalog: RuleCatalogItem[] = [];
  tenants: TenantSummary[] = [];
  allocation: TenantAllocation[] = [];
  coverage: TenantRuleAllocationSummary[] = [];
  selectedRuleCodes: string[] = [];
  allocationLoaded = false;
  loadingAllocation = false;
  removingRule = '';
  tenantsLoading = false;
  tenantLoadError = '';
  allocationLoadError = '';
  catalogLoading = false;
  catalogLoadError = '';
  coverageLoading = false;
  coverageLoadError = '';
  onboardingLoading = false;
  allocatingLoading = false;

  private api = inject(ApiService);
  private toast = inject(ToastService);
  private changeDetector = inject(ChangeDetectorRef);

  ngOnInit() {
    this.loadCatalog();
    this.loadTenants();
    this.loadCoverage();
  }

  get groupedCoverage() {
    const groups = new Map<string, { tenantId: string; bankName: string; isActive: boolean; rules: string[] }>();
    for (const item of this.coverage) {
      const current = groups.get(item.tenantId) || {
        tenantId: item.tenantId,
        bankName: item.bankName,
        isActive: item.isActive !== false,
        rules: []
      };
      if (item.ruleCode) current.rules.push(item.ruleCode);
      groups.set(item.tenantId, current);
    }
    return [...groups.values()];
  }

  get filteredCatalog() {
    const search = this.ruleSearch.trim().toLowerCase();
    return this.catalog.filter(rule => !search || `${rule.ruleCode} ${rule.description || ''}`.toLowerCase().includes(search));
  }

  get suggestedTenantCode(): string {
    return (this.tenant.tenantCode || '').trim().replace(/\s+/g, '_');
  }

  applySuggestedTenantCode() {
    this.tenant.tenantCode = this.suggestedTenantCode;
  }

  loadCatalog() {
    this.catalogLoading = true;
    this.api.ruleCatalog().subscribe({
      next: data => {
        this.catalog = [...data];
        this.catalogLoading = false;
        this.changeDetector.detectChanges();
      },
      error: error => {
        this.catalogLoading = false;
        this.catalogLoadError = this.apiErrorMessage(error, 'Rule catalog unavailable.');
        this.toast.show(this.catalogLoadError, 'danger');
        this.changeDetector.detectChanges();
      }
    });
  }

  loadTenants() {
    this.tenantsLoading = true;
    this.api.tenantsList().subscribe({
      next: data => {
        this.tenants = [...data];
        this.tenantsLoading = false;
        this.changeDetector.detectChanges();
      },
      error: error => {
        this.tenantsLoading = false;
        this.tenantLoadError = this.apiErrorMessage(error, 'Tenant API unavailable.');
        this.toast.show(this.tenantLoadError, 'danger');
        this.changeDetector.detectChanges();
      }
    });
  }

  loadCoverage() {
    this.coverageLoading = true;
    this.api.allTenantAllocations().subscribe({
      next: data => {
        this.coverage = [...data];
        this.coverageLoading = false;
        this.coverageLoadError = '';
        this.changeDetector.detectChanges();
      },
      error: error => {
        this.coverageLoading = false;
        this.coverageLoadError = this.apiErrorMessage(error, 'Rule coverage unavailable.');
        this.toast.show(this.coverageLoadError, 'danger');
        this.changeDetector.detectChanges();
      }
    });
  }

  chooseTenant(tenantId: string) {
    this.allocationTenant = tenantId;
    this.lookupTenant = tenantId;
    this.allocation = [];
    this.allocationLoaded = false;
    document.getElementById('allocation-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  isSelected(ruleCode: string) {
    return this.selectedRuleCodes.includes(ruleCode);
  }

  toggleRule(ruleCode: string) {
    this.selectedRuleCodes = this.isSelected(ruleCode)
      ? this.selectedRuleCodes.filter(code => code !== ruleCode)
      : [...this.selectedRuleCodes, ruleCode];
  }

  selectAll() {
    this.selectedRuleCodes = [...new Set([...this.selectedRuleCodes, ...this.filteredCatalog.map(rule => rule.ruleCode)])];
  }

  clearSelection() {
    this.selectedRuleCodes = [];
  }

  isTenantId(value: string) {
    return isTenantId(value);
  }

  validTenant() {
    return isTenantId(this.tenant.tenantCode) &&
      this.tenant.bankName.trim().length >= 2 &&
      isUsername(this.tenant.adminUsername) &&
      isEmail(this.tenant.adminEmail);
  }

  onboard(form: any) {
    if (this.onboardingLoading) return;
    if (form?.invalid || !this.validTenant()) {
      form?.control?.markAllAsTouched?.();
      if (this.tenant.tenantCode && this.tenant.tenantCode.includes(' ')) {
        this.toast.show(`Tenant code cannot contain spaces. Use underscores or hyphens instead (e.g. "${this.suggestedTenantCode}").`, 'danger');
      } else if (!isTenantId(this.tenant.tenantCode)) {
        this.toast.show('Please enter a valid tenant code (letters, numbers, hyphens, and underscores only).', 'danger');
      } else if (!this.tenant.bankName || this.tenant.bankName.trim().length < 2) {
        this.toast.show('Bank name is required and must be at least 2 characters.', 'danger');
      } else if (!isUsername(this.tenant.adminUsername)) {
        this.toast.show('Admin username can only contain letters, numbers, dots, hyphens, and underscores.', 'danger');
      } else if (!isEmail(this.tenant.adminEmail)) {
        this.toast.show('Please enter a valid administrator email address.', 'danger');
      } else {
        this.toast.show('Please fill in all required fields correctly.', 'danger');
      }
      return;
    }

    this.onboardingLoading = true;
    this.api.tenants({
      ...this.tenant,
      tenantCode: this.tenant.tenantCode.trim(),
      bankName: this.tenant.bankName.trim(),
      adminUsername: this.tenant.adminUsername.trim(),
      adminEmail: this.tenant.adminEmail.trim()
    }).subscribe({
      next: () => {
        this.onboardingLoading = false;
        this.toast.show('Tenant onboarded.');
        form?.resetForm?.();
        this.tenant = { tenantCode: '', bankName: '', adminUsername: '', adminEmail: '' };
        this.loadTenants();
        this.changeDetector.detectChanges();
      },
      error: error => {
        this.onboardingLoading = false;
        this.toast.show(error.error?.message || 'Could not onboard tenant.', 'danger');
        this.changeDetector.detectChanges();
      }
    });
  }

  allocate(form: any) {
    const tenantId = this.allocationTenant.trim();
    if (form?.invalid || !isTenantId(tenantId) || !this.selectedRuleCodes.length || this.selectedRuleCodes.some(code => !isRuleCode(code))) {
      return;
    }
    const existingRules = new Set(
      this.coverage
        .filter(item => item.tenantId === tenantId)
        .map(item => item.ruleCode)
        .filter((code): code is string => !!code)
    );
    const duplicateRules = this.selectedRuleCodes.filter(code => existingRules.has(code));
    if (duplicateRules.length) {
      this.toast.show(`${duplicateRules.join(', ')} ${duplicateRules.length === 1 ? 'is' : 'are'} already allocated to ${tenantId}.`, 'danger');
      return;
    }
    this.allocatingLoading = true;
    this.api.allocateRules({ tenantId, ruleCodes: this.selectedRuleCodes }).subscribe({
      next: () => {
        this.allocatingLoading = false;
        this.toast.show('Selected rules allocated.');
        this.lookupTenant = tenantId;
        this.loadAllocation();
        this.loadCoverage();
        this.changeDetector.detectChanges();
      },
      error: error => {
        this.allocatingLoading = false;
        this.toast.show(this.apiErrorMessage(error, 'Could not allocate rules.'), 'danger');
        this.changeDetector.detectChanges();
      }
    });
  }

  loadAllocation() {
    const tenantId = this.lookupTenant.trim();
    if (!isTenantId(tenantId) || this.loadingAllocation) return;
    this.loadingAllocation = true;
    this.changeDetector.detectChanges();
    this.api.tenantAllocation(tenantId).subscribe({
      next: data => {
        this.allocation = [...data];
        this.allocationLoaded = true;
        this.loadingAllocation = false;
        this.allocationLoadError = '';
        this.changeDetector.detectChanges();
      },
      error: error => {
        this.allocation = [];
        this.allocationLoaded = false;
        this.loadingAllocation = false;
        this.allocationLoadError = error.error?.message || `Could not load allocation (${error.status || 'network error'}).`;
        this.changeDetector.detectChanges();
      }
    });
  }

  remove(ruleCode: string) {
    const cleanRuleCode = ruleCode.trim();
    const tenantId = this.lookupTenant.trim();
    if (!cleanRuleCode || !isTenantId(tenantId) || this.removingRule) return;
    this.removingRule = cleanRuleCode;
    this.api.removeTenantRule(tenantId, cleanRuleCode).subscribe({
      next: () => {
        this.allocation = this.allocation.filter(rule => (rule.ruleCode || '').trim() !== cleanRuleCode);
        this.removingRule = '';
        this.toast.show('Rule removed.');
        this.loadCoverage();
        this.changeDetector.detectChanges();
      },
      error: error => {
        this.removingRule = '';
        this.toast.show(this.apiErrorMessage(error, 'Could not remove rule.'), 'danger');
        this.changeDetector.detectChanges();
      }
    });
  }

  private apiErrorMessage(error: any, fallback: string) {
    const message = error?.error?.message || error?.message;
    if (message) return message;
    if (error?.status === 409) return 'This rule is already allocated to the selected tenant.';
    return fallback;
  }
}