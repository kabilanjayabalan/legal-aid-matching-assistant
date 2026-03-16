CREATE INDEX IF NOT EXISTS idx_matches_case_id
ON matches(case_id);

CREATE INDEX IF NOT EXISTS idx_matches_provider_id
ON matches(provider_id);

CREATE INDEX IF NOT EXISTS idx_matches_status
ON matches(status);
