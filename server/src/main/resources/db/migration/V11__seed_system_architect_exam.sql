insert into exam (code, name, status)
select 'SYSTEM_ARCHITECT_DESIGNER', '系统架构设计师', 'ACTIVE'
where not exists (
    select 1 from exam where name = '系统架构设计师'
)
on conflict (code) do nothing;
