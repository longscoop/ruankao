create table app_user (
    id bigserial primary key,
    display_name varchar(120),
    avatar_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table auth_identity (
    id bigserial primary key,
    user_id bigint not null references app_user(id) on delete cascade,
    provider varchar(32) not null,
    subject varchar(160) not null,
    union_id varchar(160),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_auth_identity_provider_subject unique (provider, subject)
);

create table auth_session (
    id uuid primary key,
    user_id bigint not null references app_user(id) on delete cascade,
    token_hash char(64) not null unique,
    expires_at timestamptz not null,
    last_used_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);

create index idx_auth_identity_user on auth_identity(user_id);
create index idx_auth_session_user_expires on auth_session(user_id, expires_at);
