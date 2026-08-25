ALTER TABLE courses
    ADD COLUMN processing_progress INT UNSIGNED NOT NULL DEFAULT 0 AFTER duration_seconds;
