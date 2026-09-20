create table course (
    id bigserial primary key,
    exam_id bigint not null references exam(id) on delete cascade,
    title varchar(160) not null,
    description text,
    status varchar(32) not null,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint ck_course_title check (btrim(title) <> ''),
    constraint ck_course_status check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    constraint ck_course_sort_order check (sort_order >= 0)
);

create table course_chapter (
    id bigserial primary key,
    course_id bigint not null references course(id) on delete cascade,
    title varchar(160) not null,
    description text,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_course_chapter_sort unique (course_id, sort_order),
    constraint ck_course_chapter_title check (btrim(title) <> ''),
    constraint ck_course_chapter_sort_order check (sort_order >= 0)
);

create table video (
    id bigserial primary key,
    chapter_id bigint not null references course_chapter(id) on delete cascade,
    title varchar(200) not null,
    description text,
    object_key varchar(512) not null,
    duration_seconds integer not null,
    free_flag boolean not null default false,
    status varchar(32) not null,
    sort_order integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uq_video_chapter_sort unique (chapter_id, sort_order),
    constraint ck_video_title check (btrim(title) <> ''),
    constraint ck_video_object_key check (btrim(object_key) <> ''),
    constraint ck_video_duration check (duration_seconds > 0),
    constraint ck_video_status check (
        status in ('DRAFT', 'PROCESSING', 'WAITING_REVIEW', 'PUBLISHED', 'FAILED', 'ARCHIVED')
    ),
    constraint ck_video_sort_order check (sort_order >= 0)
);

create table video_knowledge_relation (
    id bigserial primary key,
    video_id bigint not null references video(id) on delete cascade,
    knowledge_id bigint not null references knowledge_point(id) on delete restrict,
    created_at timestamptz not null default now(),
    constraint uq_video_knowledge unique (video_id, knowledge_id)
);

create table user_video_progress (
    user_id bigint not null,
    video_id bigint not null references video(id) on delete cascade,
    progress_seconds integer not null default 0,
    completion_rate numeric(5,2) not null default 0,
    completed boolean not null default false,
    mastery_applied boolean not null default false,
    last_watch_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (user_id, video_id),
    constraint ck_video_progress_seconds check (progress_seconds >= 0),
    constraint ck_video_completion_rate check (completion_rate between 0 and 100)
);

create table video_transcript_segment (
    id bigserial primary key,
    video_id bigint not null references video(id) on delete cascade,
    start_second numeric(10,3) not null,
    end_second numeric(10,3) not null,
    content text not null,
    created_at timestamptz not null default now(),
    constraint ck_transcript_start check (start_second >= 0),
    constraint ck_transcript_range check (end_second > start_second),
    constraint ck_transcript_content check (btrim(content) <> '')
);

create index idx_course_exam
    on course(exam_id, sort_order, id);

create index idx_chapter_course
    on course_chapter(course_id, sort_order, id);

create index idx_video_chapter
    on video(chapter_id, sort_order, id);

create index idx_video_knowledge_knowledge
    on video_knowledge_relation(knowledge_id, video_id);

create index idx_video_progress_user
    on user_video_progress(user_id, updated_at desc);

create index idx_transcript_video
    on video_transcript_segment(video_id, start_second, id);
