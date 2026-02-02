CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS question (
    id uuid PRIMARY KEY,
    source varchar(16),
    subject varchar(64),
    difficulty varchar(8),
    body text,
    correct text
);

CREATE TABLE IF NOT EXISTS assignment (
    id uuid PRIMARY KEY,
    course_id uuid,
    title text
);

CREATE TABLE IF NOT EXISTS assignment_question (
    assignment_id uuid,
    question_id uuid REFERENCES question(id) ON DELETE CASCADE,
    variant int DEFAULT 0,
    points int DEFAULT 1,
    ordering int DEFAULT 0
);

CREATE TABLE IF NOT EXISTS answer (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id uuid,
    question_id uuid,
    student_id uuid,
    score int DEFAULT 0,
    answered_at timestamp DEFAULT now()
);