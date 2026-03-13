# ADR 0003: Use stateless JWT authentication

## Status

Accepted

## Context

The platform needs role-aware access control for instructors and students without introducing server-side session storage as a core dependency.

## Decision

Authenticate users against relational tables and issue stateless HMAC-signed JWT access tokens carrying:
- subject user identifier
- username
- roles
- optional instructor identifier
- optional student identifier

## Consequences

- Horizontal scaling stays simple for the current backend service.
- Service APIs can authorize requests based on token claims.
- Token revocation remains limited until a more advanced identity strategy is added.
- Claim naming and actor-identifier semantics must stay consistent across services.
