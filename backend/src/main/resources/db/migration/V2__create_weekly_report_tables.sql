CREATE TABLE weekly_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    week_start_date DATE NOT NULL,
    core_work LONGTEXT NOT NULL,
    problems LONGTEXT NULL,
    next_week_plan LONGTEXT NULL,
    source_file_name VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_weekly_reports_user_week (user_id, week_start_date),
    CONSTRAINT fk_weekly_reports_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE work_summaries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    period_type VARCHAR(16) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    core_content LONGTEXT NOT NULL,
    routine_work LONGTEXT NOT NULL,
    self_score TINYINT UNSIGNED NOT NULL,
    generated_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_work_summaries_user_period (user_id, period_type, period_start, period_end),
    CONSTRAINT chk_work_summaries_score CHECK (self_score BETWEEN 0 AND 100),
    CONSTRAINT fk_work_summaries_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
