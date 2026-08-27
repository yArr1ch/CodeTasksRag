CREATE UNIQUE INDEX IF NOT EXISTS tasks_content_unique_idx
    ON tasks (
        md5(
            lower(trim(title))
            || E'\n' || lower(trim(description))
            || E'\n' || constraints::text
            || E'\n' || test_cases::text
        )
    );
