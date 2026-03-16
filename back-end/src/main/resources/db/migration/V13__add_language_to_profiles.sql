-- Add language column to lawyer_profiles table
ALTER TABLE lawyer_profiles
ADD COLUMN language VARCHAR(100);

-- Add language column to ngo_profiles table
ALTER TABLE ngo_profiles
ADD COLUMN language VARCHAR(100);

-- Add language column to directory_lawyers table
ALTER TABLE directory_lawyers
ADD COLUMN language VARCHAR(100);

-- Add language column to directory_ngos table
ALTER TABLE directory_ngos
ADD COLUMN language VARCHAR(100);

