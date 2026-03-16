ALTER TABLE lawyer_profiles
ADD COLUMN latitude DOUBLE PRECISION,
ADD COLUMN longitude DOUBLE PRECISION;

ALTER TABLE ngo_profiles
ADD COLUMN latitude DOUBLE PRECISION,
ADD COLUMN longitude DOUBLE PRECISION;

ALTER TABLE cases
ADD COLUMN city VARCHAR(255),
ADD COLUMN latitude DOUBLE PRECISION,
ADD COLUMN longitude DOUBLE PRECISION;

-- Add latitude & longitude to directory_lawyers
ALTER TABLE directory_lawyers
ADD COLUMN latitude DOUBLE PRECISION,
ADD COLUMN longitude DOUBLE PRECISION;

-- Add latitude & longitude to directory_ngos
ALTER TABLE directory_ngos
ADD COLUMN latitude DOUBLE PRECISION,
ADD COLUMN longitude DOUBLE PRECISION;

-- Indexes for geo filtering (bounding box)
CREATE INDEX idx_directory_lawyers_lat_lng
ON directory_lawyers (latitude, longitude);

CREATE INDEX idx_directory_ngos_lat_lng
ON directory_ngos (latitude, longitude);

