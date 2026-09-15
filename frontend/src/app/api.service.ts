import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, timeout } from 'rxjs';
import { Activity, Alert, ApiResponse, BatchSummary, Officer, PageResponse, RuleCatalogItem, RuleConfig, TenantAllocation, TenantRuleAllocationSummary, TenantSummary } from './models';

const API = {
  auth: { login: '/api/v1/auth/login', masterLogin: '/api/v1/auth/master/login', resetPassword: '/api/v1/auth/reset-password' },
  tenant: {
    transactions: '/api/v1/transactions', alerts: '/api/v1/alerts', complianceAlerts: '/api/v1/compliance/alerts',
    officers: '/api/v1/bank-admin/compliance-officers', rules: '/api/v1/bank-admin/rules', assignments: '/api/v1/bank-admin/alerts', activity: '/api/v1/bank-admin/activity'
  },
  master: { tenants: '/api/v1/master/tenants', rules: '/api/v1/master/rules' }
} as const;

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient) {}

  alerts(unreviewed = false, page = 0, size = 10) {
    return this.http.get<PageResponse<Alert> | Alert[] | ApiResponse<Alert[]>>(`${unreviewed ? `${API.tenant.alerts}/unreviewed` : API.tenant.alerts}?page=${page}&size=${size}`).pipe(
      map((response) => this.pageResult(response))
    );
  }

  batchAlerts(batchId: string) { return this.http.get<Alert[]>(`${API.tenant.alerts}/batch/${encodeURIComponent(batchId)}`); }
  myBatchAlerts(batchId: string) { return this.http.get<Alert[] | ApiResponse<Alert[]>>(`${API.tenant.complianceAlerts}/batch/${encodeURIComponent(batchId)}`).pipe(map(response => this.extract<Alert[]>(response) || [])); }
  myBatchIds() { return this.http.get<string[] | ApiResponse<string[]>>(`${API.tenant.complianceAlerts}/my-batches`).pipe(map(response => this.extract<string[]>(response) || [])); }
  batches(page = 0, size = 20) { return this.http.get<PageResponse<BatchSummary>>(`${API.tenant.transactions}/batches?page=${page}&size=${size}`); }

  myAlerts(page = 0, size = 10) {
    return this.http.get<PageResponse<Alert> | ApiResponse<PageResponse<Alert>>>(`${API.tenant.complianceAlerts}/my-alerts?page=${page}&size=${size}`).pipe(map((response) => this.pageResult(this.extract(response))));
  }

  alertDetail(id: string) { return this.http.get<Alert | ApiResponse<Alert>>(`${API.tenant.complianceAlerts}/${encodeURIComponent(id)}`).pipe(map((response) => this.alertList([this.extract<Alert>(response)])[0])); }
  alertPdf(id: string, reviewNotes: string) { return this.http.post(`${API.tenant.complianceAlerts}/${encodeURIComponent(id)}/pdf`, { reviewNotes }, { responseType: 'blob' }); }
  closeAlert(id: string, reviewNotes: string) { return this.http.put<Alert | ApiResponse<Alert>>(`${API.tenant.complianceAlerts}/${encodeURIComponent(id)}/close`, { reviewNotes }).pipe(map((response) => this.extract<Alert>(response))); }
  unassignedAlerts() { return this.http.get<Alert[] | ApiResponse<Alert[]>>(`${API.tenant.assignments}/unassigned`).pipe(map((response) => this.alertList(this.extract(response)))); }
  officerAlerts(id: string) { return this.http.get<Alert[] | ApiResponse<Alert[]>>(`${API.tenant.assignments}/officer/${encodeURIComponent(id)}`).pipe(map((response) => this.alertList(this.extract(response)))); }
  officers() { return this.http.get<Officer[] | ApiResponse<Officer[]>>(API.tenant.officers).pipe(map((response) => this.extract<Officer[]>(response) || [])); }
  createOfficer(payload: object) { return this.http.post(API.tenant.officers, payload); }
  deactivateOfficer(id: string) { return this.http.put(`${API.tenant.officers}/${encodeURIComponent(id)}/deactivate`, {}); }
  reactivateOfficer(id: string) { return this.http.put(`${API.tenant.officers}/${encodeURIComponent(id)}/reactivate`, {}); }
  rules() { return this.http.get<RuleConfig[] | ApiResponse<RuleConfig[]>>(API.tenant.rules).pipe(map((response) => this.extract<RuleConfig[]>(response) || [])); }
  rule(code: string) { return this.http.get<RuleConfig | ApiResponse<RuleConfig>>(`${API.tenant.rules}/${encodeURIComponent(code)}`).pipe(map((response) => this.extract<RuleConfig>(response))); }
  updateRule(code: string, payload: object) { return this.http.put<RuleConfig | ApiResponse<RuleConfig>>(`${API.tenant.rules}/${encodeURIComponent(code)}`, payload).pipe(map((response) => this.extract<RuleConfig>(response))); }
  upload(file: File) { const form = new FormData(); form.append('file', file); return this.http.post(`${API.tenant.transactions}/upload`, form, { responseType: 'text' }); }
  assignAlerts(payload: object) { return this.http.post(`${API.tenant.assignments}/assign`, payload); }
  unassignAlerts(alertIds: string[]) { return this.http.post(`${API.tenant.assignments}/unassign`, alertIds); }
  tenants(payload: object) { return this.http.post(API.master.tenants, payload); }
  tenantsList() { return this.http.get<TenantSummary[] | ApiResponse<TenantSummary[]>>(API.master.tenants).pipe(map((response) => this.extract<TenantSummary[]>(response) || [])); }
  ruleCatalog() { return this.http.get<RuleCatalogItem[] | ApiResponse<RuleCatalogItem[]>>(`${API.master.rules}/catalog`).pipe(map((response) => this.extract<RuleCatalogItem[]>(response) || [])); }
  tenantAllocation(tenantId: string) { return this.http.get<TenantAllocation[] | ApiResponse<TenantAllocation[]>>(`${API.master.rules}/tenant/${encodeURIComponent(tenantId)}`).pipe(timeout({ first: 3000 }), map((response) => (this.extract<TenantAllocation[]>(response) || []).map((item: TenantAllocation & { rule_code?: string; rule_name?: string; allocated_at?: string }) => ({ ...item, ruleCode: item.ruleCode || item.rule_code, description: item.description || item.rule_name, allocatedAt: item.allocatedAt || item.allocated_at })))); }
  allTenantAllocations() { return this.http.get<TenantRuleAllocationSummary[] | ApiResponse<TenantRuleAllocationSummary[]>>(`${API.master.rules}/all`).pipe(map((response) => (this.extract<TenantRuleAllocationSummary[]>(response) || []).map((item: TenantRuleAllocationSummary & { tenant_id?: string; bank_name?: string; is_active?: boolean; rule_code?: string; rule_name?: string; allocated_at?: string }) => ({ ...item, tenantId: item.tenantId || item.tenant_id || '', bankName: item.bankName || item.bank_name || item.tenantId || item.tenant_id || '', isActive: item.isActive ?? item.is_active, ruleCode: item.ruleCode || item.rule_code, ruleName: item.ruleName || item.rule_name, allocatedAt: item.allocatedAt || item.allocated_at })))); }
  allocateRules(payload: object) { return this.http.post(`${API.master.rules}/allocate`, payload); }
  removeTenantRule(tenantId: string, ruleCode: string) { return this.http.delete(`${API.master.rules}/tenant/${encodeURIComponent(tenantId)}/${encodeURIComponent(ruleCode)}`); }
  activity(page = 0, size = 15) { return this.http.get<PageResponse<Activity>>(`${API.tenant.activity}?page=${page}&size=${size}`); }

  private extract<T>(response: any): T {
    if (response && typeof response === 'object' && 'data' in response && response.data !== undefined) {
      return response.data as T;
    }
    return response as T;
  }

  private alertList(response: any) {
    const data = this.extract<Alert[]>(response);
    return (Array.isArray(data) ? data : []).map((alert: Alert) => {
      const raw = alert as Alert & Record<string, unknown>;
      return {
        ...alert,
        alertId: alert.alertId || raw['alert_id'] as string,
        ruleCode: alert.ruleCode || raw['rule_code'] as string,
        ruleName: alert.ruleName || raw['rule_name'] as string,
        transactionId: alert.transactionId || raw['transaction_id'] as string,
        customerId: alert.customerId || raw['customer_id'] as string,
        triggeredAmount: alert.triggeredAmount ?? raw['triggered_amount'] as number,
        detectionMetadataJson: alert.detectionMetadataJson || raw['detection_metadata_json'] as string,
        reviewed: alert.reviewed ?? raw['is_reviewed'] as boolean,
        assignedOfficerId: alert.assignedOfficerId || raw['assigned_officer_id'] as string,
        reviewedAt: alert.reviewedAt || raw['reviewed_at'] as string,
        reviewedBy: alert.reviewedBy || raw['reviewed_by'] as string,
        reviewDecision: alert.reviewDecision || raw['review_decision'] as string,
        reviewNotes: alert.reviewNotes || raw['review_notes'] as string,
        createdAt: alert.createdAt || raw['created_at'] as string
      };
    });
  }

  private pageResult(response: any): PageResponse<Alert> {
    const res = this.extract(response) as any;
    if (res && !Array.isArray(res) && 'content' in res) return { ...res, content: this.alertList(res.content) };
    const data = this.alertList(res);
    return { content: data, totalElements: data.length, totalPages: data.length ? 1 : 0, number: 0, size: data.length };
  }
}
