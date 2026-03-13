# Local Deployment

## Purpose

The local deployment layout keeps development reproducible while exercising the real distributed shape of the system.

## Compose layout

The repository currently uses a single `docker-compose.yml` file for the main local stack.

## Containers

- `postgres`
- `redpanda`
- `service-registry`
- `config-server`
- `api-gateway`
- `backend`
- `question-provider`
- `llm-bridge`

## Runtime wiring

- `service-registry` provides Eureka discovery
- `config-server` serves externalized configuration
- `api-gateway` is the public HTTP entry point
- `backend` handles authentication and generation session orchestration
- `question-provider` handles worker-style processing and persistence
- `llm-bridge` exposes internal question generation over HTTP
- `postgres` stores application data
- `redpanda` carries asynchronous generation traffic

## Local development strategy

The project favors real infrastructure containers over heavy mocks:
- PostgreSQL runs as a real database
- Kafka-compatible messaging runs through Redpanda
- the LLM bridge defaults to a stub-friendly local mode in development

This keeps the generation flow testable without requiring paid LLM access.

## Configuration notes

- The config server reads files from the repository `config-repo` directory
- the gateway depends on both Eureka and the config server
- services use Spring Boot configuration together with environment overrides
- the LLM bridge has separate development and production profiles for stub versus real LLM usage

## Testing notes

Integration tests also rely on containerized PostgreSQL through Testcontainers.
This is intentionally separate from the Docker Compose stack so that service-level integration tests remain self-contained.

## Operational caveats

The repository is already runnable as a local microservices workspace, but some service-to-service integration details are still evolving.
That is expected for a project whose current codebase is focused on one rapidly iterated business flow.
