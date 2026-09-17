ALTER TABLE courses DROP CHECK chk_courses_duration;
ALTER TABLE courses ADD CONSTRAINT chk_courses_duration CHECK (duration_seconds <= 9000);
ALTER TABLE courses ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'RECORDING';
ALTER TABLE courses ADD COLUMN original_audio_path VARCHAR(500) NULL;
ALTER TABLE courses ADD COLUMN expected_bytes BIGINT UNSIGNED NULL;
ALTER TABLE course_audio_parts ADD COLUMN transcript LONGTEXT NULL;
