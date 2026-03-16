CREATE INDEX idx_profiles_city ON profiles(city);
CREATE INDEX idx_profiles_state ON profiles(state);
CREATE INDEX idx_profiles_verified ON profiles(is_verified);

CREATE INDEX idx_cases_status ON cases(status);
CREATE INDEX idx_cases_category ON cases(category_id);

CREATE INDEX idx_expertise_name ON expertise(name);
