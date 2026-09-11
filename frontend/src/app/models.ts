export interface ApiResponse<T> {
  success?: boolean;
  message?: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first?: boolean;
  last?: boolean;
}
export interface Activity { logId: string; userId?: string; actionType: string; affectedRecordId?: string; ipAddress?: string; details?: string; createdAt?: string; }

export interface LoginResponse {
  token: string;
  isTemporaryPassword?: boolean;
  message?: string;
}

export interface Alert {
  alertId?: string;
  ruleCode?: string;
  ruleName?: string;
  severity?: string;
  transactionId?: string;
  customerId?: string;
  triggeredAmount?: number;
  narrative?: string;
  detectionMetadataJson?: string;
  reviewed?: boolean;
  assignedCaseId?: string;
  batchId?: string;
  tenantId?: string;
  assignedOfficerId?: string;
  reviewedAt?: string;
  reviewedBy?: string;
  reviewDecision?: string;
  reviewNotes?: string;
  createdAt?: string;
}

export interface Officer {
  id?: string;
  userId?: string;
  username: string;
  email: string;
  fullName: string;
  role?: string;
  isActive?: boolean;
  isLocked?: boolean;
  createdAt?: string;
}

export interface RuleConfig {
  ruleCode: string;
  configId?: string;
  description?: string;
  isEnabled?: boolean;
  thresholdAmount?: number;
  windowMinutes?: number;
  maxCount?: number;
  percentageDeviation?: number;
  customParametersJson?: string;
}

export interface RuleCatalogItem { ruleCode: string; description?: string; [key: string]: unknown; }
export interface TenantAllocation { ruleCode?: string; description?: string; allocatedAt?: string; [key: string]: unknown; }
export interface TenantSummary { tenantId: string; bankName: string; isActive: boolean; createdAt?: string; }
export interface TenantRuleAllocationSummary { tenantId: string; bankName: string; isActive?: boolean; ruleCode?: string; ruleName?: string; allocatedAt?: string; }
export interface BatchSummary { batchId: string; fileName?: string; status?: string; uploadedAt?: string; }
