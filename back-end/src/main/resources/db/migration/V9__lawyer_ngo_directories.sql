ALTER TABLE cases
    ADD COLUMN category_id BIGINT,
ADD COLUMN assigned_profile_id BIGINT,
ADD COLUMN status VARCHAR(30) DEFAULT 'OPEN';

ALTER TABLE cases
    ADD CONSTRAINT fk_case_category
        FOREIGN KEY (category_id)
            REFERENCES categories(id);

ALTER TABLE cases
    ADD CONSTRAINT fk_case_assigned_profile
        FOREIGN KEY (assigned_profile_id)
            REFERENCES profiles(id);
