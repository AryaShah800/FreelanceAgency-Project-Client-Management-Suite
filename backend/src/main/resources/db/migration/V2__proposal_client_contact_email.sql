ALTER TABLE proposals
    ADD COLUMN client_email VARCHAR(254);

CREATE INDEX idx_proposals_agency_client_email
    ON proposals (agency_id, client_email);
