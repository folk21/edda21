-- Creates "student" table that links 1:1 to "user" table.
-- This table is used for mapping authenticated users to student entities.

create table if not exists student (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null unique,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index if not exists idx_student_user_id
    on student(user_id);

-- Optional foreign key for referential integrity.
-- Comment out if your existing schema conflicts with this.
alter table student
    add constraint fk_student_user
        foreign key (user_id) references "user"(id)
        on delete cascade;
