alter table app_user
    add column if not exists role varchar(32) not null default 'USER';

update app_user
set role = case
    when upper(trim(role)) = 'ADMIN' then 'ADMIN'
    else 'USER'
end;

alter table app_user
    drop constraint if exists ck_app_user_role;

alter table app_user
    add constraint ck_app_user_role check (role in ('USER', 'ADMIN'));

create index if not exists idx_app_user_role on app_user(role);
