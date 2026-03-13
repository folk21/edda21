# ADR 0001: Use Spring Cloud microservices

## Status

Accepted

## Context

The project is intended to model a distributed EdTech platform rather than a single deployable backend.
The repository already separates gateway, discovery, configuration, business services, and the LLM bridge.

## Decision

Use multiple Spring Boot services with Spring Cloud infrastructure components:
- API Gateway
- Eureka Service Registry
- Config Server
- business services per runtime responsibility

## Consequences

- Runtime boundaries are explicit from the start.
- Local deployment is more complex than a monolith.
- Service-to-service contracts and configuration management become first-class concerns.
- The architecture is well aligned with later scaling and service extraction needs.
