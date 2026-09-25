create table kb_base (
    id uuid primary key,
    exam_id bigint not null references exam(id),
    name varchar(120) not null,
    description varchar(1000) not null default '',
    created_at timestamptz not null default now()
);

create table kb_document (
    id uuid primary key,
    base_id uuid not null references kb_base(id),
    filename varchar(240) not null,
    sha256 char(64) not null,
    page_count integer not null check (page_count between 1 and 500),
    chunk_count integer not null check (chunk_count > 0),
    status varchar(20) not null default 'REVIEW' check (status in ('REVIEW','PUBLISHED')),
    warnings jsonb not null default '[]',
    uploaded_by bigint not null references app_user(id),
    created_at timestamptz not null default now(),
    unique (base_id, sha256)
);

create table kb_chunk (
    id bigserial primary key,
    document_id uuid not null references kb_document(id) on delete cascade,
    page integer not null check (page > 0),
    ordinal integer not null check (ordinal >= 0),
    text text not null check (length(text) between 1 and 800),
    search_vector tsvector not null,
    unique (document_id, ordinal)
);
create index idx_kb_chunk_search on kb_chunk using gin(search_vector);
create index idx_kb_document_base_status on kb_document(base_id, status);

create table kb_agent (
    id uuid primary key,
    exam_id bigint not null references exam(id),
    name varchar(120) not null,
    description varchar(1000) not null default '',
    instructions varchar(2000) not null default '',
    enabled boolean not null default false,
    created_at timestamptz not null default now()
);
create table kb_agent_base (
    agent_id uuid not null references kb_agent(id) on delete cascade,
    base_id uuid not null references kb_base(id),
    primary key (agent_id, base_id)
);
create index idx_kb_agent_base_base on kb_agent_base(base_id);

create table kb_session (
    id uuid primary key,
    user_id bigint not null references app_user(id) on delete cascade,
    agent_id uuid not null references kb_agent(id),
    title varchar(80) not null default '新会话',
    busy_token uuid,
    busy_until timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check ((busy_token is null) = (busy_until is null))
);
create index idx_kb_session_owner on kb_session(user_id, agent_id, updated_at desc);

create table kb_turn (
    id bigserial primary key,
    session_id uuid not null references kb_session(id) on delete cascade,
    request_id uuid not null,
    question varchar(2000) not null,
    content text not null check (length(content) <= 16000),
    status varchar(20) not null check (status in ('GROUNDED','EXTRACT_ONLY','NO_EVIDENCE')),
    citations jsonb not null default '[]',
    created_at timestamptz not null default now(),
    unique (session_id, request_id)
);
create index idx_kb_turn_session on kb_turn(session_id, id);
