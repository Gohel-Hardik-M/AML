import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, timeout } from 'rxjs';
import { Activity, Alert, BatchSummary, Officer, PageResponse, RuleCatalogItem, RuleConfig, TenantAllocation, TenantRuleAllocationSummary, TenantSummary } from './models';

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
    return this.http.get<PageResponse<Alert> | Alert[]>(`${unreviewed ? `${API.tenant.alerts}/unreviewed` : API.tenant.alerts}?page=${page}&size=${size}`).pipe(
      map((response) => this.pageResult(response))
    );
  }

  batchAlerts(batchId: string) { return this.http.get<Alert[]>(`${API.tenant.alerts}/batch/${encodeURIComponent(batchId)}`); }
  myBatchAlerts(batchId: string) { return this.http.get<Alert[]>(`${API.tenant.complianceAlerts}/batch/${encodeURIComponent(batchId)}`); }
  myBatchIds() { return this.http.get<string[]>(`${API.tenant.complianceAlerts}/my-batches`); }
  batches(page = 0, size = 20) { return this.http.get<PageResponse<BatchSummary>>(`${API.tenant.transactions}/batches?page=${page}&size=${size}`); }

  myAlerts(page = 0, size = 10) {
    return this.http.get<PageResponse<Alert>>(`${API.tenant.complianceAlerts}/my-alerts?page=${page}&size=${size}`).pipe(
      map((response) => this.pageResult(response))
    );
  }

  alertDetail(id: string) {
    return this.http.get<Alert>(`${API.tenant.complianceAlerts}/${encodeURIComponent(id)}`).pipe(
      map((response) => this.alertList([response])[0])
    );
  }

  alertPdf(id: string, reviewNotes: string) { return this.http.post(`${API.tenant.complianceAlerts}/${encodeURIComponent(id)}/pdf`, { reviewNotes }, { responseType: 'blob' }); }
  closeAlert(id: string, reviewNotes: string) { return this.http.put<Alert>(`${API.tenant.complianceAlerts}/${encodeURIComponent(id)}/close`, { reviewNotes }); }
  unassignedAlerts() { return this.http.get<Alert[]>(`${API.tenant.assignments}/unassigned`).pipe(map((response) => this.alertList(response))); }
  officerAlerts(id: string) { return this.http.get<Alert[]>(`${API.tenant.assignments}/officer/${encodeURIComponent(id)}`).pipe(map((response) => this.alertList(response))); }
  officers() { return this.http.get<Officer[]>(API.tenant.officers); }
  createOfficer(payload: object) { return this.http.post(API.tenant.officers, payload); }
  deactivateOfficer(id: string) { return this.http.put(`${API.tenant.officers}/${encodeURIComponent(id)}/deactivate`, {}); }
  reactivateOfficer(id: string) { return this.http.put(`${API.tenant.officers}/${encodeURIComponent(id)}/reactivate`, {}); }
  rules() { return this.http.get<RuleConfig[]>(API.tenant.rules); }
  rule(code: string) { return this.http.get<RuleConfig>(`${API.tenant.rules}/${encodeURIComponent(code)}`); }
  updateRule(code: string, payload: object) { return this.http.put<RuleConfig>(`${API.tenant.rules}/${encodeURIComponent(code)}`, payload); }
  upload(file: File) { const form = new FormData(); form.append('file', file); return this.http.post(`${API.tenant.transactions}/upload`, form, { responseType: 'text' }); }
  assignAlerts(payload: object) { return this.http.post(`${API.tenant.assignments}/assign`, payload); }
  unassignAlerts(alertIds: string[]) { return this.http.post(`${API.tenant.assignments}/unassign`, alertIds); }
  tenants(payload: object) { return this.http.post(API.master.tenants, payload); }
  tenantsList() { return this.http.get<TenantSummary[]>(API.master.tenants); }
  ruleCatalog() { return this.http.get<RuleCatalogItem[]>(`${API.master.rules}/catalog`); }
  tenantAllocation(tenantId: string) {
    return this.http.get<TenantAllocation[]>(`${API.master.rules}/tenant/${encodeURIComponent(tenantId)}`).pipe(
      timeout({ first: 3000 }),
      map((response) => (response || []).map((item: TenantAllocation & { rule_code?: string; rule_name?: string; allocated_at?: string }) => ({
        ...item,
        ruleCode: item.ruleCode || item.rule_code,
        description: item.description || item.rule_name,
        allocatedAt: item.allocatedAt || item.allocated_at
      })))
    );
  }
  allTenantAllocations() {
    return this.http.get<TenantRuleAllocationSummary[]>(`${API.master.rules}/all`).pipe(
      map((response) => (response || []).map((item: TenantRuleAllocationSummary & { tenant_id?: string; bank_name?: string; is_active?: boolean; rule_code?: string; rule_name?: string; allocated_at?: string }) => ({
        ...item,
        tenantId: item.tenantId || item.tenant_id || '',
        bankName: item.bankName || item.bank_name || item.tenantId || item.tenant_id || '',
        isActive: item.isActive ?? item.is_active,
        ruleCode: item.ruleCode || item.rule_code,
        ruleName: item.ruleName || item.rule_name,
        allocatedAt: item.allocatedAt || item.allocated_at
      })))
    );
  }
  allocateRules(payload: object) { return this.http.post(`${API.master.rules}/allocate`, payload); }
  removeTenantRule(tenantId: string, ruleCode: string) { return this.http.delete(`${API.master.rules}/tenant/${encodeURIComponent(tenantId)}/${encodeURIComponent(ruleCode)}`); }
  activity(page = 0, size = 15) { return this.http.get<PageResponse<Activity>>(`${API.tenant.activity}?page=${page}&size=${size}`); }

  private alertList(data: any): Alert[] {
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
    if (response && !Array.isArray(response) && 'content' in response) {
      return { ...response, content: this.alertList(response.content) };
    }
    const data = this.alertList(response);
    return { content: data, totalElements: data.length, totalPages: data.length ? 1 : 0, number: 0, size: data.length };
  }
}
