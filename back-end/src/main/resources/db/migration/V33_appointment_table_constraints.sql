ALTER TABLE appointments
ALTER COLUMN match_id TYPE INT
USING match_id::INT;

ALTER TABLE appointments
ALTER COLUMN receiver_id TYPE INT
USING receiver_id::INT;

ALTER TABLE appointments
ALTER COLUMN requester_id TYPE INT
USING requester_id::INT;

ALTER TABLE appointments
ADD CONSTRAINT fk_appointments_match
FOREIGN KEY (match_id)
REFERENCES matches(id)
ON DELETE CASCADE;

ALTER TABLE appointments
ADD CONSTRAINT fk_appointments_receiver
FOREIGN KEY (receiver_id)
REFERENCES users(id)
ON DELETE RESTRICT;

ALTER TABLE appointments
ALTER COLUMN last_modified_by 
TYPE BIGINT USING last_modified_by::BIGINT;

ALTER TABLE appointments
ADD CONSTRAINT fk_appointments_requester
FOREIGN KEY (requester_id)
REFERENCES users(id)
ON DELETE RESTRICT;
