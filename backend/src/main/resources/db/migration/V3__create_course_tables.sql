CREATE TABLE courses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL,
    duration_seconds INT UNSIGNED NOT NULL DEFAULT 0,
    transcript LONGTEXT NULL,
    note_content LONGTEXT NULL,
    error_message VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_courses_user_created (user_id, created_at),
    CONSTRAINT fk_courses_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_courses_duration CHECK (duration_seconds <= 5400)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE course_audio_parts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    part_number INT UNSIGNED NOT NULL,
    duration_seconds INT UNSIGNED NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    file_size BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_audio_parts_number (course_id, part_number),
    CONSTRAINT fk_course_audio_parts_course FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
