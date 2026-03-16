CREATE TABLE lawyer_directory (
                                  profile_id BIGINT PRIMARY KEY,
                                  bar_registration_no VARCHAR(100) UNIQUE,
                                  years_of_experience INT,
                                  CONSTRAINT fk_lawyer_profile FOREIGN KEY (profile_id)
                                      REFERENCES profiles(id) ON DELETE CASCADE
);

CREATE TABLE ngo_directory (
                               profile_id BIGINT PRIMARY KEY,
                               ngo_registration_no VARCHAR(100) UNIQUE,
                               CONSTRAINT fk_ngo_profile FOREIGN KEY (profile_id)
                                   REFERENCES profiles(id) ON DELETE CASCADE
);
