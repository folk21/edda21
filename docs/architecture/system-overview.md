# System Overview

edda21 is a Java and Spring Boot microservices system for an EdTech platform with asynchronous question generation.

The current repository already contains the runtime backbone for:
- JWT-based authentication in the backend service
- instructor-driven question generation sessions
- question selection from PostgreSQL
- asynchronous dispatch through Kafka or Redpanda
- an HTTP-based LLM bridge implemented in Java with Spring AI
- session status polling for the UI

The broader product direction is larger than the current implementation.
The initial requirements also describe course management, student work on assignments, grading, and analytics.
Those capabilities are documented in `docs/product` as target-state requirements when they are not yet fully present in the codebase.

## Runtime topology

- API Gateway
- Backend service
- Question Provider service
- LLM Bridge service
- Service Registry
- Config Server
- PostgreSQL
- Redpanda

## Architectural posture

The codebase is not a generic CRUD monolith.
It is centered around one business-critical flow: preparing assignment questions from two sources:
- existing questions stored in PostgreSQL
- newly generated questions returned by an LLM-backed service

The repository currently contains two closely related generation styles:

1. A session-based orchestration flow in the backend service:
   - create a `question_generation_session`
   - select any reusable questions from the database
   - publish a Kafka request for the missing amount
   - expose status by REST

2. A direct worker-style flow in the question-provider service:
   - consume a Kafka request
   - optionally load questions from the database
   - call the LLM bridge by HTTP
   - persist and link questions to the assignment

This means the architecture is already usable for the generation scenario, but some parts are still converging toward a single canonical end-to-end flow.

## Primary synchronous flows

- login and JWT issuance
- create generation session
- read generation session status
- read assignment together with linked questions
- internal HTTP call from question-provider to llm-bridge

## Primary asynchronous flows

- publish question generation request to Kafka
- consume generation request in question-provider
- optional follow-up processing of LLM response payloads and session update

## Data ownership at a high level

- `backend` owns authentication, security, and generation session orchestration
- `question-provider` owns question persistence, assignment-question linking, and worker logic
- `llm-bridge` owns prompt loading and question generation adapters
- `db-schema` owns Flyway migrations for the shared relational schema

## Current implementation scope

Clearly represented in the current codebase:
- authentication against `user`, `instructor`, and `student`
- generation session lifecycle and polling
- database-backed question reuse
- LLM-backed question generation through stub or Spring AI

Documented in requirements but not yet broadly represented in controllers and services:
- instructor registration
- student registration
- course creation and enrollment management
- student assignment attempt lifecycle
- answer persistence API
- grading flows
- analytical projections and dashboards
