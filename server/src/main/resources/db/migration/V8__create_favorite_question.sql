create table favorite_question (
    user_id bigint not null,
    question_id bigint not null references question(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, question_id)
);

create index idx_favorite_question_user_created
    on favorite_question(user_id, created_at desc);
