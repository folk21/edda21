# Edda21 Microservices (Java-only) — Spring Cloud + Kafka (Redpanda) + Postgres + Spring AI

A microservices project for an EdTech-like platform.
It uses:

- **Spring Cloud** patterns: Config Server, Eureka Service Registry, API Gateway.
- **Kafka** (via **Redpanda**) for async communications between services.
- **PostgreSQL** for persistent storage with **Flyway** migrations.
- **Spring AI** (Java) as an LLM bridge (no Python in this repo):
  - `generator.mode=stub` (deterministic, no credentials)
  - `generator.mode=llm` (OpenAI via Spring AI)


### Architecture and Product Documentation

Additional project documentation is available in the `docs/` directory.

- `docs/architecture/`
    - system overview
    - module map
    - runtime flows
    - local deployment notes
    - quality attributes
    - ADRs
    - Structurizr workspace files
- `docs/product/`
    - product vision
    - business requirements
    - use cases

---

## 1) Project description

Edda21 is a compact Java-based microservices system that integrates Large Language Models (LLMs) into an Edda21 scenario
for automatic question generation, with a strong focus on clean architecture and transparent infrastructure.

The **LLM bridge** service (`llm-bridge`) is built on **Java** and **Spring Boot**, using a **hexagonal
(ports-and-adapters) architecture** to separate domain logic from technical details. Core domain workflows depend on
small, explicit interfaces (ports), while all framework-specific and infrastructure code lives in adapters.

Integration with the LLM is handled through **Spring AI**. The `ChatExecutor` port defines a thin abstraction over
the Spring AI client:

```java
String execute(String system, String user, OpenAiChatOptions options);
```

This hides the concrete LLM client and configuration from the rest of the application. Application services construct
the system/user prompts and options (model, temperature, etc.) and delegate to `ChatExecutor`. Because only this
interface is visible to the domain/application layer, it is easy to switch the underlying LLM provider or mock it in tests.

Question generation itself is represented by the `QuestionGeneratorClient` port. This interface models a high-level
operation:

```java
List<QuestionDTO> generate(
    String subject,
    String topic,
    String difficulty,
    QuestionType qtype,
    int count,
    String locale);
```

Implementations of `QuestionGeneratorClient` may use LLMs via `ChatExecutor`, rule-based logic, or external HTTP APIs.
From the perspective of the rest of the system, they are just “question generators” that return structured `QuestionDTO`
objects.

---

## 2) Services

**Platform (Spring Cloud):**

- `service-registry` — Eureka (8761)
- `config-server` — Config Server (8888)
- `api-gateway` — API Gateway (8080)

**Domain services:**

- `backend` — auth + assignments + question generation sessions (8082)
- `question-provider` — DB persistence + Kafka consumers (8083)
- `llm-bridge` — LLM-backed question generation (8098)

**Shared modules:**

- `db-schema` — Flyway migrations shared as a dependency
- `test-support` — shared test helpers

---

## 3) Prerequisites

- **Java 21**, **Gradle 8+**
- **Docker** + **Docker Compose**
- Optional: `psql` client (for manual DB checks)

**Ports in use (typical setup):**

- 8080 — API Gateway
- 8761 — Eureka
- 8888 — Config Server
- 8082 — backend
- 8083 — question-provider
- 8098 — llm-bridge
- 9092 — Kafka / Redpanda
- 5432 — Postgres

---

## 4) Build & packaging

Run all commands from the Gradle root (the directory that contains `settings.gradle` and `docker-compose.yml`).

### 4.1 Build JARs

```bash
./gradlew \
  :services:backend:bootJar \
  :services:question-provider:bootJar \
  :services:llm-bridge:bootJar \
  :platform:api-gateway:bootJar \
  :platform:service-registry:bootJar \
  :platform:config-server:bootJar
```

### 4.2. Testcontainers with OrbStack on macOS

Some integration tests use Testcontainers and require a working local Docker engine.

If you use OrbStack, switching the Docker CLI context may be enough for terminal commands, 
but Testcontainers running from IntelliJ IDEA or Gradle may still fail with:

```text
Could not find a valid Docker environment
```

If this happens, create a ~/.testcontainers.properties file with the following content:
```bash
docker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy
docker.host=unix\:///Users/<your-user>/.orbstack/run/docker.sock
```

### 4.3 Build Docker images (tags unified to `edda21/*:dev`)

Important: `backend` and `question-provider` Dockerfiles copy `build/libs/*.jar`, so their build context must be the
service directory (not the repo root).

```bash
# backend + question-provider (context = service directory)
docker build -t edda21/backend:dev           -f services/backend/Dockerfile services/backend
docker build -t edda21/question-provider:dev -f services/question-provider/Dockerfile services/question-provider

# llm-bridge + platform services (Dockerfiles expect repo-root context)
docker build -t edda21/llm-bridge:dev        -f services/llm-bridge/Dockerfile .
docker build -t edda21/api-gateway:dev       -f platform/api-gateway/Dockerfile .
docker build -t edda21/service-registry:dev  -f platform/service-registry/Dockerfile .
docker build -t edda21/config-server:dev     -f platform/config-server/Dockerfile .
```

---

## 5) Configuration

### 5.1 Docker Compose environment (recommended)

`docker-compose.yml` wires services together and provides defaults.

Key variables you should keep consistent with the code:

- **Kafka** (used by `backend` producer and `question-provider` consumers)
  - `KAFKA_BOOTSTRAP=redpanda:9092`

- **Postgres**
  - `POSTGRES_DB=edda21`
  - `POSTGRES_USER=postgres`
  - `POSTGRES_PASSWORD=postgres`
  - `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/edda21` (for services that use Spring datasource directly)

- **Question-provider → LLM bridge HTTP**
  - `LLM_BASE_URL=http://llm-bridge:8098`
    - maps to property `llm.base-url`
    - required in Docker (otherwise the default points to `localhost`)

- **LLM Bridge mode**
  - `GENERATOR_MODE=stub` (default, deterministic)
  - `GENERATOR_MODE=llm` (OpenAI via Spring AI)

- **OpenAI (only when `GENERATOR_MODE=llm`)**
  - `SPRING_AI_OPENAI_API_KEY=<your_key>`
  - `SPRING_AI_MODEL=gpt-4o-mini` (optional override)

### 5.2 Gateway routes (Config Server)

The gateway reads routes from `config-repo/api-gateway-docker.yml` (mounted into `config-server`).

For a working “single entrypoint” setup via gateway, ensure you route at least:

- `backend`: `/auth/**`, `/assignments/**`, `/question-sessions/**`
- (optional) `llm-bridge`: `/llm/**` for manual testing

Example config:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: backend
          uri: lb://backend
          predicates:
            - Path=/auth/**,/assignments/**,/question-sessions/**
        - id: llm-bridge
          uri: lb://llm-bridge
          predicates:
            - Path=/llm/**
```

If you do not add these routes, you can still call services directly by their ports (e.g. backend on `:8082`).

---

## 6) Run the stack

```bash
docker compose up -d
docker compose ps
```

(Optional) Explicitly create topics on Redpanda (manual setup):

```bash
docker compose exec redpanda rpk topic create questions.request questions.response questions.dlq
```

**Useful endpoints (local):**

```text
Eureka:        http://localhost:8761
Config Server: http://localhost:8888/actuator/health
Gateway:       http://localhost:8080/actuator/health
Backend:       http://localhost:8082/actuator/health
LLM Bridge:    http://localhost:8098/actuator/health
```

---

## 7) Bootstrap demo data (one-time)

There is no “register” endpoint; auth validates users against the database.
For local demo you can create a single instructor user + one assignment.

Below assumes `POSTGRES_DB=edda21` and default credentials.

```bash
docker compose exec -T postgres psql -U postgres -d edda21 <<'SQL'
-- Clean up if re-running the script
delete from instructor where user_id in (select id from "user" where username = 'instructor1');
delete from "user" where username = 'instructor1';

-- Create an INSTRUCTOR user:
-- password = "secret"
-- bcrypt hash was generated with cost=10; any valid BCrypt hash is fine.
with u as (
  insert into "user"(username, password_hash, role, enabled)
  values (
    'instructor1',
    '$2b$10$RrUb06IZRwz57zeuPXuqG.sIfnDbdctTgbZAHBbVCgNQ8ubFdcmHG',
    'INSTRUCTOR',
    true
  )
  returning id
)
insert into instructor(user_id)
select id from u;

-- Create a demo assignment (course_id is a UUID placeholder; there is no course table in V1 schema).
insert into assignment(id, course_id, title)
values (
  '00000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000010',
  'Demo assignment'
)
on conflict (id) do update set title = excluded.title;

-- Seed a few questions so DB selection can pick them up in DB_ONLY / DB_THEN_LLM modes.
insert into question(id, source, subject, difficulty, body, correct)
values
  (gen_random_uuid(), 'BANK', 'MATH', 'B1', 'What is 2+2?', '4'),
  (gen_random_uuid(), 'BANK', 'MATH', 'B1', 'What is 3+5?', '8')
on conflict (id) do nothing;
SQL
```

If you want to generate your own BCrypt hash quickly:

```bash
# Linux/macOS (apache2-utils):
# htpasswd -bnBC 10 "" secret | tr -d ':\n' ; echo
```

---

## 8) Test the system

### 8.1 Login (backend)

If you configured the gateway routes (section 5.2), use `:8080`. Otherwise call backend directly on `:8082`.

```bash
BASE_URL=http://localhost:8080  # or http://localhost:8082

curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"instructor1","password":"secret"}'
```

The response contains:

```json
{ "accessToken": "<jwt>", "role": "INSTRUCTOR" }
```

### 8.2 Create a question generation session (DB_ONLY)

This mode completes immediately and is the simplest end-to-end check.

```bash
TOKEN="<paste_accessToken_here>"
ASSIGNMENT_ID=00000000-0000-0000-0000-000000000001
COURSE_ID=00000000-0000-0000-0000-000000000010

curl -s -X POST "$BASE_URL/question-sessions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "courseId": "'"$COURSE_ID"'",
        "assignmentId": "'"$ASSIGNMENT_ID"'",
        "requestedCount": 2,
        "mode": "DB_ONLY",
        "filters": {
          "subject": "MATH",
          "difficulty": "B1"
        }
      }'
```

### 8.3 Read session status

```bash
SESSION_ID="<paste_sessionId_here>"

curl -s -X GET "$BASE_URL/question-sessions/$SESSION_ID" \
  -H "Authorization: Bearer $TOKEN"
```

There is also a convenience endpoint:

```bash
curl -s -X GET "$BASE_URL/question-sessions/by-assignment/$ASSIGNMENT_ID" \
  -H "Authorization: Bearer $TOKEN"
```

### 8.4 Test LLM Bridge directly

The LLM bridge exposes:

- `POST /llm/generate` → returns a JSON array of `QuestionDTO`

```bash
curl -s -X POST http://localhost:8098/llm/generate \
  -H "Content-Type: application/json" \
  -d '{
        "subject": "MATH",
        "topic": "algebra",
        "difficulty": "B1",
        "type": "MULTI_CHOICE",
        "count": 3,
        "locale": "en"
      }'
```

When `GENERATOR_MODE=stub`, the output is deterministic and requires no API key.
When `GENERATOR_MODE=llm`, the service calls OpenAI via Spring AI.

### 8.5 DB_THEN_LLM / LLM_ONLY session notes (WIP wiring)

`backend` produces a Kafka request with this Java payload shape:

- `sessionId`, `courseId`, `assignmentId`
- `requestedCount`, `missingCount`
- `mode` (DB_ONLY / DB_THEN_LLM / LLM_ONLY)
- `filters` (free-form map)

Session completion requires an LLM worker to publish a response to `questions.response` in the
`LlmQuestionsResponsePayload` shape (consumed by `question-provider`'s LLM result processor).

If you are only running the Java services from this repo, prefer `DB_ONLY` for a fully completed session,
and test `llm-bridge` directly (section 8.4).

---

## 9) Switching LLM modes

In `docker-compose.yml`, for `llm-bridge`:

```yaml
environment:
  GENERATOR_MODE: stub
```

To enable OpenAI:

```yaml
environment:
  GENERATOR_MODE: llm
  SPRING_AI_OPENAI_API_KEY: ${SPRING_AI_OPENAI_API_KEY}
  SPRING_AI_MODEL: gpt-4o-mini
```

Restart only the bridge:

```bash
docker compose up -d --force-recreate llm-bridge
```

---

## 10) Running tests

Run the full test suite:

```bash
./gradlew test
```

Or target a module:

```bash
./gradlew :services:backend:test
./gradlew :services:question-provider:test
./gradlew :services:llm-bridge:test
```

---

## 11) Scaling consumers

Simulate a consumer group with multiple instances:

```bash
docker compose up -d --scale question-provider=3
```

Kafka will assign partitions to instances in the same group.

---

## 12) Troubleshooting

- **Gateway 404**: ensure the gateway routes exist in `config-repo/api-gateway-docker.yml` and `config-server` is healthy.
- **Kafka connection errors**: ensure `KAFKA_BOOTSTRAP=redpanda:9092` is set for services and `redpanda` is healthy.
- **LLM bridge returns stub data**: check `GENERATOR_MODE` is `llm` and `SPRING_AI_OPENAI_API_KEY` is present.
- **Auth errors**: verify the demo user exists in the DB and the password is a valid BCrypt hash.

---

## 13) Clean up

```bash
docker compose down        # stop containers
docker compose down -v     # also remove Postgres data volume
```
