-- Profiles search
CREATE INDEX idx_profiles_city ON profiles(city);
CREATE INDEX idx_profiles_state ON profiles(state);
CREATE INDEX idx_profiles_verified ON profiles(is_verified);

-- Expertise search
CREATE INDEX idx_expertise_name ON expertise(name);

-- Cases filtering
CREATE INDEX idx_cases_status ON cases(status);
CREATE INDEX idx_cases_category ON cases(category_id);
CREATE INDEX idx_cases_assigned_profile ON cases(assigned_profile_id);

-- Composite search index
CREATE INDEX idx_profiles_city_state ON profiles(city, state);
