ALTER TABLE courses
    ADD COLUMN course_name VARCHAR(160) NULL,
    ADD COLUMN lesson_date DATE NULL;

UPDATE courses SET course_name = title, lesson_date = DATE(created_at);

ALTER TABLE courses
    MODIFY COLUMN course_name VARCHAR(160) NOT NULL,
    MODIFY COLUMN lesson_date DATE NOT NULL;
