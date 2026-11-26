ALTER TABLE agency_invitations DROP CONSTRAINT IF EXISTS agency_invitations_role_check;
ALTER TABLE agency_invitations ADD CONSTRAINT agency_invitations_role_check CHECK (role IN ('ADMIN', 'MEMBER', 'CLIENT'));
