# Business Requirements

## Actors

- Instructor
- Student
- Admin, reserved for later operational or support concerns

## Scope note

This document combines two layers:
- currently visible capabilities in the repository
- target-state capabilities described by the initial requirements

Where a capability is not yet broadly implemented in controllers and services, it is still listed here because it is part of the intended product scope.

## Business capabilities

### Authentication and identity

- Users can authenticate with username and password.
- The system issues JWT access tokens.
- Tokens carry role information.
- Tokens may also carry instructor or student identifiers for downstream authorization.
- Instructor and student registration are part of the target product scope.

### Question generation

- Instructors can request question preparation for an assignment.
- The system supports three source modes:
  - `DB_ONLY`
  - `DB_THEN_LLM`
  - `LLM_ONLY`
- The system stores a question-generation session with counters and result codes.
- The generation status is observable through dedicated REST endpoints.
- Questions can be reused from the database before new questions are generated.

### Assignment content

- Questions are persisted in a relational store.
- Questions are linked to assignments through an assignment-question relation.
- The assignment content can be read together with linked questions.
- Variants and provenance are important design concerns for the product direction even when not yet fully represented across all flows.

### Course and enrollment management

Target-state business capabilities:
- instructors can create courses
- instructors can add students to courses
- instructors can open a course and view its assignments
- students can open their courses and see available assignments

### Student work and grading

Target-state business capabilities:
- students can start work on an assignment
- students can save answers
- students can submit assignments
- instructors can grade completed work
- students can see grades for an assignment

### Analytics

Target-state business capabilities:
- engagement analytics
- content difficulty and performance analysis
- LLM generation quality analytics
- infrastructure and operational analytics
- pedagogical insights such as difficult topics or risk indicators

## Business rules

- A generation request is tracked as a session with status and counters.
- A generation session belongs to an instructor and is associated with an assignment.
- An assignment belongs to one course in the target model.
- Questions linked to an assignment must be persisted before they are exposed as assignment content.
- Role-based access control must prevent instructors from acting outside their allowed scope.
- Student-facing work should become immutable after final submission in the target model.
- Analytics should not overload the same store that serves hot transactional traffic.

## Non-functional requirements

- Java-first implementation style
- Docker-based local development
- Kafka-compatible asynchronous messaging
- PostgreSQL with versioned schema migrations
- stateless JWT authentication
- replaceable LLM integration
- strong testability through unit tests and container-backed integration tests
