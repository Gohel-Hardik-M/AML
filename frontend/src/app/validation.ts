export const TENANT_ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;
export const USERNAME_PATTERN = /^[A-Za-z0-9._-]{1,64}$/;
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[A-Za-z]{2,63}$/;
export const RULE_CODE_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;

export function isTenantId(value: string) {
  return TENANT_ID_PATTERN.test(value.trim());
}

export function isUsername(value: string) {
  return USERNAME_PATTERN.test(value.trim());
}

export function isEmail(value: string) {
  return EMAIL_PATTERN.test(value.trim());
}

export function isRuleCode(value: string) {
  return RULE_CODE_PATTERN.test(value.trim());
}

export function isPassword(value: string) {
  return /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&+=!])\S{10,128}$/.test(value);
}

export function isJsonObject(value: string) {
  if (!value.trim()) return true;
  try {
    const parsed = JSON.parse(value);
    return parsed !== null && typeof parsed === 'object' && !Array.isArray(parsed);
  } catch {
    return false;
  }
}
