CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    keycloak_subject VARCHAR(128) NOT NULL UNIQUE,
    display_name VARCHAR(160) NOT NULL,
    points INTEGER NOT NULL DEFAULT 0 CHECK (points >= 0),
    total_earned INTEGER NOT NULL DEFAULT 0 CHECK (total_earned >= 0),
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE point_transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    type VARCHAR(32) NOT NULL,
    amount INTEGER NOT NULL,
    task_id UUID REFERENCES tasks(id),
    submission_id UUID REFERENCES submissions(id),
    idempotency_key VARCHAR(180) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX point_transactions_user_created_idx
    ON point_transactions (user_id, created_at DESC);

CREATE TABLE user_task_progress (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id),
    task_id UUID NOT NULL REFERENCES tasks(id),
    hint_used BOOLEAN NOT NULL DEFAULT FALSE,
    reference_unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    passed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT user_task_progress_user_task_unique UNIQUE (user_id, task_id)
);

ALTER TABLE submissions ADD COLUMN user_id UUID REFERENCES app_users(id);

CREATE INDEX submissions_user_idx ON submissions (user_id);
