-- Creates core auth tables: "user" and "instructor".
-- Adjust names/types if your existing schema differs.

create table if not exists "user" (
    id uuid primary key default gen_random_uuid(),
    username varchar(255) not null unique,
    password_hash varchar(255) not null,
    role varchar(32) not null,
    enabled boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index if not exists idx_user_username
    on "user"(username);

create table if not exists instructor (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null unique,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index if not exists idx_instructor_user_id
    on instructor(user_id);

-- Optional foreign key if you want strict referential integrity.
-- Comment out if your existing data model conflicts with this.
alter table instructor
    add constraint fk_instructor_user
        foreign key (user_id) references "user"(id)
        on delete cascade;
