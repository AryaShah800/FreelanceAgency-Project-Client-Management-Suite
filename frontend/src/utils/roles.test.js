import assert from 'node:assert/strict';
import test from 'node:test';
import { normalizeRole } from './roles.js';

test('accepts every server role and the Spring ROLE_ authority form', () => {
  assert.equal(normalizeRole('OWNER'), 'OWNER');
  assert.equal(normalizeRole('ROLE_ADMIN'), 'ADMIN');
  assert.equal(normalizeRole(' member '), 'MEMBER');
  assert.equal(normalizeRole('client'), 'CLIENT');
});

test('missing and unknown roles do not receive a privileged fallback', () => {
  for (const role of [null, undefined, '', 'SUPERUSER', 'ROLE_ROLE_OWNER', 1]) {
    assert.equal(normalizeRole(role), null);
  }
});
