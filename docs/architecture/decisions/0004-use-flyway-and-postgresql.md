# ADR 0004: Use Flyway and PostgreSQL

## Status

Accepted

## Context

The core business flow depends on durable relational data:
- reusable question storage
- assignment-question linking
- generation session status
- authenticated user records

The project also needs schema evolution that can be reviewed and replayed across environments.

## Decision

Use PostgreSQL as the primary relational store and Flyway for schema migrations.

## Consequences

- The data model remains explicit and versioned.
- Integration tests can validate real schema behavior against PostgreSQL.
- Shared-schema coordination across services must be managed carefully.
- Migration discipline becomes part of normal development work.
