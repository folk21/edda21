-- Creates table for tracking question generation sessions
-- used by backend and LLM result processor.

create table if not exists question_generation_session (
    id uuid primary key default gen_random_uuid(),
    instructor_id uuid not null,
    course_id uuid,
    assignment_id uuid,

    requested_count int not null,
    db_selected_count int not null default 0,
    llm_generated_count int not null default 0,

    mode varchar(32) not null,
    status varchar(32) not null,
    result_code varchar(64),
    error_message text,

    filter_json text,

    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index if not exists idx_qgs_assignment_id
    on question_generation_session(assignment_id);

create index if not exists idx_qgs_instructor_id
    on question_generation_session(instructor_id);
