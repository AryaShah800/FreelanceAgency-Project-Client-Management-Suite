const VALID_ROLES = new Set(['OWNER', 'ADMIN', 'MEMBER', 'CLIENT']);

export const normalizeRole = (role) => {
  if (typeof role !== 'string') return null;
  const normalized = role.trim().replace(/^ROLE_/i, '').trim().toUpperCase();
  return VALID_ROLES.has(normalized) ? normalized : null;
};
