# Quality Attributes

## Modifiability

The repository is split into small Gradle modules with explicit responsibilities.
The most important business flow is isolated into separate services:
- backend orchestration
- question persistence and worker logic
- LLM integration

This reduces accidental coupling and makes it easier to evolve one concern at a time.

## Testability

The codebase already includes:
- unit tests for JWT token generation
- unit tests for prompt-driven LLM mapping
- integration tests backed by a real PostgreSQL container
- shared test-support utilities

The architecture is intentionally friendly to black-box service tests and database-backed integration tests.

## Local reproducibility

Local development does not depend on managed cloud services.
The project uses:
- Docker Compose for the full local stack
- PostgreSQL as a real local database
- Redpanda for Kafka-compatible messaging
- a stub-capable LLM bridge for zero-cost development mode

## Security baseline

The backend service already applies:
- stateless JWT authentication
- role-based authorization
- BCrypt password verification
- explicit extraction of instructor and student identities from token claims

This is a solid baseline for the current instructor-focused generation scenario.

## Clear operational boundaries

The Spring Cloud platform modules make runtime boundaries visible:
- routing at the gateway
- service discovery through Eureka
- central configuration through the config server
- asynchronous work through Kafka or Redpanda

This gives the project an architecture that matches its intended production direction instead of hiding everything in one process.

## Persistence clarity

PostgreSQL schema evolution is tracked through Flyway migrations.
The schema already models:
- questions
- assignments
- assignment-question links
- question generation sessions
- users
- instructors
- students

That makes the storage model reviewable and versioned.

## Known design tension

The repository currently contains overlapping generation paths:
- a session-first orchestration flow
- a direct worker-to-LLM HTTP flow
- a response-topic processing path that is still maturing

This is not necessarily wrong for an evolving codebase, but it is the main architectural area that should be simplified as the project stabilizes.
