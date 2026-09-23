create table question_session (
    id uuid primary key,
    user_id bigint not null,
    exam_id bigint not null references exam(id) on delete restrict,
    source varchar(32) not null,
    knowledge_id bigint references knowledge_point(id) on delete restrict,
    created_at timestamptz not null default now(),
    constraint ck_question_session_source check (
        source in ('CHAPTER', 'REAL_EXAM', 'WRONG_REVIEW', 'AI_QUIZ')
    )
);

create table question_session_item (
    id bigserial primary key,
    session_id uuid not null references question_session(id) on delete cascade,
    question_id bigint not null references question(id) on delete restrict,
    sort_order integer not null,
    created_at timestamptz not null default now(),
    constraint uq_question_session_item unique (session_id, question_id),
    constraint uq_question_session_sort unique (session_id, sort_order)
);

create index idx_question_session_user_created on question_session(user_id, created_at desc);
create index idx_question_session_item_session on question_session_item(session_id, sort_order);
