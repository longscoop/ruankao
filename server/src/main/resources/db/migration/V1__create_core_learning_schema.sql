create table exam (
    id bigserial primary key,
    code varchar(64) not null,
    name varchar(128) not null,
    status varchar(32) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table knowledge_point (
    id bigserial primary key,
    exam_id bigint not null references exam(id) on delete cascade,
    parent_id bigint references knowledge_point(id) on delete restrict,
    level smallint not null,
    code varchar(64) not null,
    name varchar(160) not null,
    description text,
    importance smallint not null,
    exam_frequency numeric(5,2) not null,
    estimated_minutes integer not null,
    sort_order integer not null default 0,
    status varchar(32) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table question (
    id bigserial primary key,
    exam_id bigint not null references exam(id) on delete cascade,
    type varchar(32) not null,
    source varchar(32) not null,
    status varchar(32) not null,
    difficulty varchar(32) not null,
    content text not null,
    options_json jsonb,
    standard_answer text,
    explanation text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table question_knowledge (
    id bigserial primary key,
    question_id bigint not null references question(id) on delete cascade,
    knowledge_id bigint not null references knowledge_point(id) on delete restrict,
    weight numeric(8,6) not null,
    primary_flag boolean not null default false,
    created_at timestamptz not null default now()
);

create table answer_record (
    id uuid primary key,
    user_id bigint not null,
    question_id bigint not null references question(id) on delete restrict,
    session_id uuid,
    answer text,
    correct boolean not null,
    duration_seconds integer,
    confidence varchar(32),
    source varchar(32) not null,
    mastery_applied boolean not null default false,
    answered_at timestamptz not null default now()
);

create table user_knowledge_mastery (
    user_id bigint not null,
    knowledge_id bigint not null references knowledge_point(id) on delete cascade,
    mastery_score numeric(6,2) not null default 0,
    evidence_count integer not null default 0,
    correct_streak integer not null default 0,
    wrong_streak integer not null default 0,
    last_effective_study_at timestamptz,
    updated_at timestamptz not null default now(),
    primary key (user_id, knowledge_id)
);

create table wrong_question (
    id bigserial primary key,
    user_id bigint not null,
    question_id bigint not null references question(id) on delete cascade,
    status varchar(32) not null,
    wrong_count integer not null default 1,
    consecutive_correct integer not null default 0,
    first_wrong_at timestamptz not null default now(),
    last_wrong_at timestamptz not null default now(),
    mastered_at timestamptz,
    updated_at timestamptz not null default now()
);
