create table ai_usage_log (
    id uuid primary key,
    user_id bigint not null,
    request_id uuid not null unique,
    provider varchar(64) not null,
    model varchar(128) not null,
    function varchar(64) not null,
    prompt_tokens integer not null default 0,
    completion_tokens integer not null default 0,
    latency_ms bigint not null,
    estimated_cost numeric(14,6) not null default 0,
    success boolean not null,
    error_code varchar(128),
    created_at timestamptz not null default now()
);

create index idx_ai_usage_user_created on ai_usage_log(user_id, created_at desc);
