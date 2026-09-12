import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiService],
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('normalizes snake_case alert response fields to camelCase', () => {
    service.alerts(false, 0, 10).subscribe((result) => {
      expect(result.content.length).toBe(1);
      const item = result.content[0];
      expect(item.alertId).toBe('alt-snake-1');
      expect(item.ruleCode).toBe('RULE_SMURF');
      expect(item.ruleName).toBe('Smurfing Detection');
      expect(item.transactionId).toBe('tx-99');
      expect(item.customerId).toBe('cust-42');
      expect(item.triggeredAmount).toBe(50000);
      expect(item.detectionMetadataJson).toBe('{"window": 24}');
      expect(item.reviewed).toBe(true);
      expect(item.assignedOfficerId).toBe('off-7');
      expect(item.reviewedAt).toBe('2026-09-12T10:00:00Z');
      expect(item.reviewedBy).toBe('compliance_officer');
      expect(item.reviewDecision).toBe('CLOSED_FALSE_POSITIVE');
      expect(item.reviewNotes).toBe('Customer verified as wholesale distributor.');
      expect(item.createdAt).toBe('2026-09-12T09:30:00Z');
    });

    const req = httpMock.expectOne('/api/v1/alerts?page=0&size=10');
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        alert_id: 'alt-snake-1',
        rule_code: 'RULE_SMURF',
        rule_name: 'Smurfing Detection',
        transaction_id: 'tx-99',
        customer_id: 'cust-42',
        triggered_amount: 50000,
        detection_metadata_json: '{"window": 24}',
        is_reviewed: true,
        assigned_officer_id: 'off-7',
        reviewed_at: '2026-09-12T10:00:00Z',
        reviewed_by: 'compliance_officer',
        review_decision: 'CLOSED_FALSE_POSITIVE',
        review_notes: 'Customer verified as wholesale distributor.',
        created_at: '2026-09-12T09:30:00Z'
      }
    ]);
  });

  it('unassignedAlerts calls /api/v1/bank-admin/alerts/unassigned', () => {
    service.unassignedAlerts().subscribe((alerts) => {
      expect(alerts.length).toBe(1);
      expect(alerts[0].alertId).toBe('alt-101');
    });

    const req = httpMock.expectOne('/api/v1/bank-admin/alerts/unassigned');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, data: [{ alertId: 'alt-101', ruleCode: 'VELOCITY_001' }] });
  });

  it('createOfficer posts payload to /api/v1/bank-admin/compliance-officers', () => {
    const payload = { fullName: 'John Doe', username: 'jdoe', email: 'jdoe@bank.com' };
    service.createOfficer(payload).subscribe();

    const req = httpMock.expectOne('/api/v1/bank-admin/compliance-officers');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush({ success: true });
  });

  it('deactivateOfficer and reactivateOfficer PUT to bank-admin endpoints', () => {
    service.deactivateOfficer('user-1').subscribe();
    const req1 = httpMock.expectOne('/api/v1/bank-admin/compliance-officers/user-1/deactivate');
    expect(req1.request.method).toBe('PUT');
    req1.flush({ success: true });

    service.reactivateOfficer('user-1').subscribe();
    const req2 = httpMock.expectOne('/api/v1/bank-admin/compliance-officers/user-1/reactivate');
    expect(req2.request.method).toBe('PUT');
    req2.flush({ success: true });
  });

  it('rule and updateRule communicate with /api/v1/bank-admin/rules/{code}', () => {
    service.rule('RULE_001').subscribe((rule) => {
      expect(rule.ruleCode).toBe('RULE_001');
    });
    const req1 = httpMock.expectOne('/api/v1/bank-admin/rules/RULE_001');
    expect(req1.request.method).toBe('GET');
    req1.flush({ success: true, data: { ruleCode: 'RULE_001', thresholdAmount: 10000 } });

    service.updateRule('RULE_001', { thresholdAmount: 20000 }).subscribe();
    const req2 = httpMock.expectOne('/api/v1/bank-admin/rules/RULE_001');
    expect(req2.request.method).toBe('PUT');
    expect(req2.request.body).toEqual({ thresholdAmount: 20000 });
    req2.flush({ success: true, data: { ruleCode: 'RULE_001', thresholdAmount: 20000 } });
  });

  it('assignAlerts and unassignAlerts send assignment requests to backend', () => {
    service.assignAlerts({ officerId: 'officer-1', alertIds: ['a1', 'a2'] }).subscribe();
    const req1 = httpMock.expectOne('/api/v1/bank-admin/alerts/assign');
    expect(req1.request.method).toBe('POST');
    expect(req1.request.body).toEqual({ officerId: 'officer-1', alertIds: ['a1', 'a2'] });
    req1.flush({ success: true });

    service.unassignAlerts(['a1', 'a2']).subscribe();
    const req2 = httpMock.expectOne('/api/v1/bank-admin/alerts/unassign');
    expect(req2.request.method).toBe('POST');
    expect(req2.request.body).toEqual(['a1', 'a2']);
    req2.flush({ success: true });
  });

  it('closeAlert and alertPdf hit compliance endpoints', () => {
    service.closeAlert('alt-1', 'Reviewed and resolved').subscribe();
    const req1 = httpMock.expectOne('/api/v1/compliance/alerts/alt-1/close');
    expect(req1.request.method).toBe('PUT');
    expect(req1.request.body).toEqual({ reviewNotes: 'Reviewed and resolved' });
    req1.flush({ success: true, data: { alertId: 'alt-1', reviewed: true } });

    service.alertPdf('alt-1', 'SAR filed with FinCEN').subscribe((blob) => {
      expect(blob).toBeTruthy();
    });
    const req2 = httpMock.expectOne('/api/v1/compliance/alerts/alt-1/pdf');
    expect(req2.request.method).toBe('POST');
    expect(req2.request.body).toEqual({ reviewNotes: 'SAR filed with FinCEN' });
    req2.flush(new Blob(['pdf content'], { type: 'application/pdf' }));
  });

  it('ruleCatalog and allocateRules hit master endpoints', () => {
    service.ruleCatalog().subscribe((catalog) => {
      expect(catalog.length).toBe(1);
      expect(catalog[0].ruleCode).toBe('RULE_GEO');
    });
    const req1 = httpMock.expectOne('/api/v1/master/rules/catalog');
    expect(req1.request.method).toBe('GET');
    req1.flush({ success: true, data: [{ ruleCode: 'RULE_GEO', ruleName: 'Geographic Risk' }] });

    service.allocateRules({ tenantId: 'BANK_B', ruleCodes: ['RULE_GEO'] }).subscribe();
    const req2 = httpMock.expectOne('/api/v1/master/rules/allocate');
    expect(req2.request.method).toBe('POST');
    expect(req2.request.body).toEqual({ tenantId: 'BANK_B', ruleCodes: ['RULE_GEO'] });
    req2.flush({ success: true });
  });
});
