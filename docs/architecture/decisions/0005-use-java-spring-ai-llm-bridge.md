# ADR 0005: Use a Java Spring AI LLM bridge

## Status

Accepted

## Context

The project needs an LLM integration point that fits the Java-first stack and remains replaceable between safe local development and real provider-backed execution.

## Decision

Expose LLM-backed question generation through a dedicated Java Spring Boot service that:
- provides an internal HTTP API
- supports a stub mode for local development
- supports a Spring AI-backed mode for real LLM calls
- keeps prompt loading and generation mapping in one service boundary

## Consequences

- The rest of the system can treat question generation as a replaceable internal service.
- Local development avoids accidental LLM cost when stub mode is enabled.
- Prompt engineering concerns stay isolated from the rest of the business services.
- The project must still define a single canonical asynchronous integration path as the architecture matures.
