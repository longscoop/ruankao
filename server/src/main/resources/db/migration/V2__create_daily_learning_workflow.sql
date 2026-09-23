create table user_exam_profile (
    user_id bigint primary key,
    exam_id bigint not null references exam(id) on delete restrict,
    exam_date date not null,
    daily_target_minutes integer not null,
    foundation_level varchar(32) not null,
    assessment_completed boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_user_exam_profile_target_minutes
        check (daily_target_minutes in (15, 30, 60, 90)),
    constraint ck_user_exam_profile_foundation
        check (foundation_level in ('ZERO', 'SOME', 'REVIEWING'))
);

create table assessment_session (
    id uuid primary key,
    user_id bigint not null,
    exam_id bigint not null references exam(id) on delete restrict,
    status varchar(32) not null,
    created_at timestamptz not null default now(),
    submitted_at timestamptz,
    constraint ck_assessment_session_status
        check (status in ('IN_PROGRESS', 'SUBMITTED'))
);

create table assessment_item (
    id bigserial primary key,
    session_id uuid not null references assessment_session(id) on delete cascade,
    question_id bigint not null references question(id) on delete restrict,
    sort_order integer not null,
    answer_record_id uuid references answer_record(id) on delete restrict,
    created_at timestamptz not null default now(),
    constraint ck_assessment_item_sort_order check (sort_order > 0),
    constraint uq_assessment_item_question unique (session_id, question_id),
    constraint uq_assessment_item_sort unique (session_id, sort_order)
);

create table study_plan (
    id bigserial primary key,
    user_id bigint not null,
    exam_id bigint not null references exam(id) on delete restrict,
    plan_date date not null,
    target_minutes integer not null,
    estimated_minutes integer not null default 0,
    actual_minutes integer not null default 0,
    completion_rate numeric(5,2) not null default 0,
    status varchar(32) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_study_plan_user_exam_date unique (user_id, exam_id, plan_date),
    constraint ck_study_plan_target_minutes check (target_minutes in (15, 30, 60, 90)),
    constraint ck_study_plan_estimated_minutes check (estimated_minutes >= 0),
    constraint ck_study_plan_actual_minutes check (actual_minutes >= 0),
    constraint ck_study_plan_completion_rate check (completion_rate between 0 and 100),
    constraint ck_study_plan_status check (status in ('ACTIVE', 'COMPLETED'))
);

create table study_task (
    id bigserial primary key,
    plan_id bigint not null references study_plan(id) on delete cascade,
    task_type varchar(32) not null,
    knowledge_id bigint references knowledge_point(id) on delete restrict,
    target_question_count integer not null default 0,
    completed_question_count integer not null default 0,
    estimated_minutes integer not null default 0,
    actual_minutes integer not null default 0,
    priority_score numeric(7,2),
    sort_order integer not null,
    status varchar(32) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_study_task_plan_sort unique (plan_id, sort_order),
    constraint ck_study_task_type
        check (task_type in ('WRONG_REVIEW', 'WEAK_POINT', 'NEW_KNOWLEDGE', 'REAL_EXAM')),
    constraint ck_study_task_target_questions check (target_question_count >= 0),
    constraint ck_study_task_completed_questions check (completed_question_count >= 0),
    constraint ck_study_task_estimated_minutes check (estimated_minutes >= 0),
    constraint ck_study_task_actual_minutes check (actual_minutes >= 0),
    constraint ck_study_task_status
        check (status in ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED'))
);

create index idx_assessment_session_user_exam
    on assessment_session(user_id, exam_id, created_at desc);

create index idx_study_plan_user_date
    on study_plan(user_id, plan_date desc);

create index idx_study_task_plan
    on study_task(plan_id, sort_order);
