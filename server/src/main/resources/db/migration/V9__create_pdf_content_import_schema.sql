alter table app_user
    add column role varchar(32) not null default 'USER';

alter table app_user
    add constraint ck_app_user_role check (role in ('USER', 'ADMIN'));

create table lesson (
    id bigserial primary key,
    chapter_id bigint not null references course_chapter(id) on delete cascade,
    title varchar(240) not null,
    summary text,
    status varchar(32) not null,
    source_import_batch_id uuid,
    source_page_start integer,
    source_page_end integer,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_lesson_status check (status in ('DRAFT','REVIEW','PUBLISHED','ARCHIVED')),
    constraint ck_lesson_source_pages check (
        (source_page_start is null and source_page_end is null)
        or (source_page_start >= 1 and source_page_end >= source_page_start)
    )
);

create table lesson_block (
    id bigserial primary key,
    lesson_id bigint not null references lesson(id) on delete cascade,
    block_type varchar(32) not null,
    text_content text,
    image_object_key text,
    source_page integer,
    sort_order integer not null,
    created_at timestamptz not null default now(),
    constraint ck_lesson_block_type check (
        block_type in ('TEXT','IMAGE','TABLE','DIAGRAM','TIP','IMPORTANT','QUESTION','CASE')
    ),
    constraint ck_lesson_block_content check (
        text_content is not null or image_object_key is not null
    ),
    constraint ck_lesson_block_page check (source_page is null or source_page >= 1),
    constraint uq_lesson_block_order unique (lesson_id, sort_order)
);

create table lesson_knowledge (
    lesson_id bigint not null references lesson(id) on delete cascade,
    knowledge_id bigint not null references knowledge_point(id) on delete restrict,
    created_at timestamptz not null default now(),
    primary key (lesson_id, knowledge_id)
);

create table content_import_batch (
    id uuid primary key,
    exam_id bigint not null references exam(id) on delete cascade,
    filename varchar(512) not null,
    original_object_key text,
    sha256 char(64) not null,
    detected_type varchar(32) not null,
    status varchar(32) not null,
    title varchar(512),
    page_count integer not null default 0,
    created_by bigint not null,
    materialized_course_id bigint references course(id) on delete set null,
    confirm_key varchar(160),
    confirmed_at timestamptz,
    published_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_import_detected_type check (
        detected_type in ('QUESTION_BANK','LECTURE','MIXED','UNKNOWN')
    ),
    constraint ck_import_status check (
        status in ('PARSED','REVIEWING','CONFIRMED','PUBLISHED','FAILED')
    ),
    constraint ck_import_page_count check (page_count >= 0),
    constraint uq_import_confirm_key unique (confirm_key)
);

create table content_import_page (
    id bigserial primary key,
    batch_id uuid not null references content_import_batch(id) on delete cascade,
    page_number integer not null,
    text_content text,
    image_object_key text,
    created_at timestamptz not null default now(),
    constraint ck_import_page_number check (page_number >= 1),
    constraint uq_import_page unique (batch_id, page_number)
);

create table content_import_item (
    id bigserial primary key,
    batch_id uuid not null references content_import_batch(id) on delete cascade,
    item_type varchar(32) not null,
    item_key varchar(160) not null,
    source_page_start integer not null,
    source_page_end integer not null,
    title varchar(512),
    content_json jsonb not null,
    status varchar(32) not null,
    target_id bigint,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_import_item_type check (item_type in ('LESSON','KNOWLEDGE','QUESTION')),
    constraint ck_import_item_status check (
        status in ('PENDING','APPROVED','REJECTED','MATERIALIZED','PUBLISHED')
    ),
    constraint ck_import_item_pages check (
        source_page_start >= 1 and source_page_end >= source_page_start
    ),
    constraint uq_import_item_key unique (batch_id, item_key)
);

create table content_import_issue (
    id bigserial primary key,
    batch_id uuid not null references content_import_batch(id) on delete cascade,
    item_id bigint references content_import_item(id) on delete cascade,
    severity varchar(16) not null,
    code varchar(80) not null,
    message text not null,
    source_page integer,
    status varchar(16) not null default 'OPEN',
    created_at timestamptz not null default now(),
    resolved_at timestamptz,
    constraint ck_import_issue_severity check (severity in ('INFO','WARNING','ERROR')),
    constraint ck_import_issue_status check (status in ('OPEN','RESOLVED')),
    constraint ck_import_issue_page check (source_page is null or source_page >= 1)
);

alter table question
    add column import_batch_id uuid,
    add column source_page integer,
    add column source_question_no varchar(80),
    add column source_label varchar(160);

alter table lesson
    add constraint fk_lesson_import_batch
    foreign key (source_import_batch_id)
    references content_import_batch(id)
    on delete set null;

alter table question
    add constraint fk_question_import_batch
    foreign key (import_batch_id)
    references content_import_batch(id)
    on delete set null;

create index idx_lesson_chapter on lesson(chapter_id, sort_order, id);
create index idx_lesson_knowledge_knowledge on lesson_knowledge(knowledge_id);
create index idx_import_batch_exam_created on content_import_batch(exam_id, created_at desc);
create index idx_import_item_batch_status on content_import_item(batch_id, status, sort_order);
create index idx_import_issue_batch_status on content_import_issue(batch_id, status, severity);
