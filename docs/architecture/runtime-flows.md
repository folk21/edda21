# Runtime Flows

## Create question generation session

```mermaid
sequenceDiagram
    participant Instructor
    participant Gateway
    participant BackendController
    participant SessionService
    participant SessionRepo
    participant QuestionSelection
    participant KafkaProducer
    participant Kafka

    Instructor->>Gateway: POST /question-sessions
    Gateway->>BackendController: forward request
    BackendController->>SessionService: createSession(request)
    SessionService->>SessionRepo: insert question_generation_session
    SessionService->>QuestionSelection: selectQuestionsFromDb(...)
    QuestionSelection-->>SessionService: selectedFromDb count
    alt missing questions remain
        SessionService->>KafkaProducer: send generation request
        KafkaProducer-->>Kafka: publish questions.request
    else all questions found in DB
        SessionService->>SessionRepo: update status COMPLETED
    end
    SessionService-->>BackendController: session response
    BackendController-->>Instructor: current session snapshot
```

## Consume generation request and call LLM bridge

```mermaid
sequenceDiagram
    participant Kafka
    participant QuestionListener
    participant QuestionRepo
    participant LlmBridge
    participant PostgreSQL

    Kafka-->>QuestionListener: questions.request
    QuestionListener->>QuestionRepo: loadFromDb(subject, count)
    alt mode is DB_ONLY and enough questions exist
        QuestionListener->>QuestionRepo: saveQuestionsForAssignment(...)
    else mode requires LLM
        QuestionListener->>LlmBridge: POST /llm/generate
        LlmBridge-->>QuestionListener: generated questions
        QuestionListener->>QuestionRepo: saveQuestionsForAssignment(...)
    end
    QuestionRepo->>PostgreSQL: insert question rows and assignment_question links
```

## Read session status

```mermaid
sequenceDiagram
    participant Instructor
    participant Gateway
    participant BackendController
    participant SessionService
    participant SessionRepo

    Instructor->>Gateway: GET /question-sessions/{sessionId}
    Gateway->>BackendController: forward request
    BackendController->>SessionService: getSession(sessionId)
    SessionService->>SessionRepo: load session
    SessionRepo-->>SessionService: session entity
    SessionService-->>BackendController: response DTO
    BackendController-->>Instructor: status, counters, resultCode
```

## Process LLM response payload and finalize a session

```mermaid
sequenceDiagram
    participant Kafka
    participant ResponseListener
    participant ResultProcessor
    participant PostgreSQL

    Kafka-->>ResponseListener: questions.response
    ResponseListener->>ResultProcessor: processResponse(payload)
    alt payload contains errorCode
        ResultProcessor->>PostgreSQL: mark session FAILED
    else payload contains generated questions
        ResultProcessor->>PostgreSQL: insert generated questions
        ResultProcessor->>PostgreSQL: insert assignment links
        ResultProcessor->>PostgreSQL: update llm_generated_count and resultCode
    end
```

## Notes on the current state

- The session-based orchestration flow is the clearest business flow in the backend service.
- The question-provider service also contains a direct request-consumer flow that calls the LLM bridge by HTTP.
- The response-topic processing flow is represented in code and tests, but the upstream publisher side is still less explicit than the direct HTTP path.
- The documentation therefore treats the repository as a generation-focused system that is still converging on one end-to-end asynchronous design.
