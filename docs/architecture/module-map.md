# Module Map

## platform:service-registry

Responsible for service discovery through Eureka.

## platform:config-server

Responsible for externalized runtime configuration loaded by Spring Cloud clients.

## platform:api-gateway

Responsible for the public entry point and route forwarding to backend services.

## services:backend

Responsible for:
- login and JWT issuing
- authenticated actor context resolution
- question generation session orchestration
- session status read APIs
- assignment-level read and generation entry points exposed to instructors

## services:question-provider

Responsible for:
- Kafka-based question-generation workers
- database question reuse
- question persistence and assignment-question linking
- loading assignments together with linked questions
- processing LLM result payloads and updating generation session counters

## services:llm-bridge

Responsible for:
- the internal HTTP API for question generation
- prompt loading and rendering
- switching between stub and Spring AI-backed generation
- mapping generated JSON into typed question DTOs

## services:db-schema

Responsible for Flyway migrations and the baseline PostgreSQL schema used by the services.

## services:test-support

Responsible for shared integration test infrastructure such as Testcontainers-backed PostgreSQL support and reusable test data helpers.

## auth and identity

Cross-cutting runtime capability implemented mostly in `services:backend`.
Includes:
- login endpoint
- password verification with BCrypt
- JWT generation and validation
- extraction of instructor and student identifiers from security context

## generation sessions

Cross-cutting business capability implemented mostly in `services:backend`.
Includes:
- `question_generation_session` persistence
- requested count tracking
- selected-from-database count tracking
- LLM-generated count tracking
- status and result-code polling for the UI

## question bank selection

Cross-cutting business capability split across backend and question-provider.
Includes:
- selecting reusable questions from PostgreSQL
- linking selected questions to assignments
- honoring source mode such as `DB_ONLY`, `DB_THEN_LLM`, and `LLM_ONLY`

## LLM integration

Cross-cutting capability implemented mainly in `services:llm-bridge` and consumed by `services:question-provider`.
Includes:
- stub generation for safe local development
- Spring AI integration for real LLM calls
- prompt templates and output-shape enforcement

## future product areas

The requirements describe additional bounded contexts that are not yet represented as full modules:
- courses
- enrollments
- assignment attempts
- submissions
- grading
- analytics
