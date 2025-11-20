# llm-bridge — prompts, DTOs, stub/llm switch

- Templates: `src/main/resources/prompts/questions/{system.st,user.st,output_spec.st}`
- DTOs: `QuestionDTO`, `QuestionSetDTO`
- Clients: `StubQuestionGeneratorClient` (default), `SpringAiQuestionGeneratorClient` (enable with `GENERATOR_MODE=llm`)
- Endpoint: POST /llm/generate