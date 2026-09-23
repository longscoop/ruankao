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


alter table exam
    add constraint uq_exam_code unique (code),
    add constraint ck_exam_status check (status in ('ACTIVE', 'INACTIVE'));

alter table knowledge_point
    add constraint uq_knowledge_exam_code unique (exam_id, code),
    add constraint ck_knowledge_level check (level >= 1),
    add constraint ck_knowledge_importance check (importance between 1 and 5),
    add constraint ck_knowledge_exam_frequency check (exam_frequency between 0 and 100),
    add constraint ck_knowledge_estimated_minutes check (estimated_minutes > 0),
    add constraint ck_knowledge_status check (status in ('DRAFT', 'ACTIVE', 'INACTIVE'));

alter table question
    add constraint ck_question_type check (type in ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'CASE', 'ESSAY')),
    add constraint ck_question_source check (source in ('REAL_EXAM', 'CHAPTER', 'SIMULATION', 'AI_GENERATED', 'MANUAL')),
    add constraint ck_question_status check (status in ('DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED')),
    add constraint ck_question_difficulty check (difficulty in ('EASY', 'MEDIUM', 'HARD'));

alter table question_knowledge
    add constraint uq_question_knowledge unique (question_id, knowledge_id),
    add constraint ck_question_knowledge_weight check (weight > 0 and weight <= 1);

create unique index uq_question_primary_knowledge
    on question_knowledge(question_id)
    where primary_flag = true;

alter table answer_record
    add constraint ck_answer_duration check (duration_seconds is null or duration_seconds >= 0),
    add constraint ck_answer_confidence check (confidence is null or confidence in ('GUESS', 'UNCERTAIN', 'CONFIDENT')),
    add constraint ck_answer_source check (source in (
        'ASSESSMENT', 'DAILY_PLAN', 'CHAPTER', 'REAL_EXAM',
        'WRONG_REVIEW', 'MOCK_EXAM', 'AI_QUIZ'
    ));

alter table user_knowledge_mastery
    add constraint ck_mastery_score check (mastery_score between 0 and 100),
    add constraint ck_mastery_evidence_count check (evidence_count >= 0),
    add constraint ck_mastery_correct_streak check (correct_streak >= 0),
    add constraint ck_mastery_wrong_streak check (wrong_streak >= 0);

alter table wrong_question
    add constraint uq_wrong_question_user_question unique (user_id, question_id),
    add constraint ck_wrong_question_status check (status in ('ACTIVE', 'MASTERED')),
    add constraint ck_wrong_question_wrong_count check (wrong_count >= 1),
    add constraint ck_wrong_question_consecutive_correct check (consecutive_correct >= 0);

create index idx_knowledge_exam on knowledge_point(exam_id);
create index idx_question_exam on question(exam_id);
create index idx_question_knowledge_question on question_knowledge(question_id);
create index idx_question_knowledge_knowledge on question_knowledge(knowledge_id);
create index idx_answer_user_answered on answer_record(user_id, answered_at desc);
create index idx_wrong_question_user_status on wrong_question(user_id, status);
