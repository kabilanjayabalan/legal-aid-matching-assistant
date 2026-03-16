CREATE TABLE expertise (
                           id BIGSERIAL PRIMARY KEY,
                           name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE profile_expertise (
                                   profile_id BIGINT NOT NULL,
                                   expertise_id BIGINT NOT NULL,
                                   PRIMARY KEY (profile_id, expertise_id),
                                   CONSTRAINT fk_pe_profile FOREIGN KEY (profile_id)
                                       REFERENCES profiles(id) ON DELETE CASCADE,
                                   CONSTRAINT fk_pe_expertise FOREIGN KEY (expertise_id)
                                       REFERENCES expertise(id) ON DELETE CASCADE
);
