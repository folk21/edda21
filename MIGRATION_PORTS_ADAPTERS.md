# Migration to Ports & Adapters (Variant 2)

This refactor reorganizes packages for `llm-bridge` and `question-provider`:

## llm-bridge
- `api/*` → `web/*` (REST controllers)
- `core/QuestionDTO, QuestionType` → `domain/model/*`
- `core/QuestionGeneratorClient` → `domain/port/out/QuestionGeneratorClient`
- `core/SpringAiQuestionGeneratorClient, SpringAiChatExecutor, ChatExecutor` → `adapter/out/llm/*`
- `core/StubQuestionGeneratorClient` → `adapter/out/stub/*`
- `core/JsonUtil, core/PromptService` → `util/*`

## question-provider
- `QuestionListener` → `adapter/in/messaging/QuestionListener`
- `QuestionRepo` → `adapter/out/persistence/QuestionRepo`, now implements `domain.port.out.QuestionWritePort`
- New outbound port: `domain/port/out/QuestionWritePort`

### Notes
- Ensure `@SpringBootApplication` scans `com.edda21.*` in each service module.
- Update imports if you had custom usages outside the standard spots.
- Reimport Gradle in IDE after unpacking.
