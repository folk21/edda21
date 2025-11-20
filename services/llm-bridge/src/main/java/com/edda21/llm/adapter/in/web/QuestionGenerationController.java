package com.edda21.llm.adapter.in.web;

import static com.edda21.llm.domain.model.QuestionType.fromJson;

import com.edda21.llm.domain.model.QuestionDTO;
import com.edda21.llm.domain.model.QuestionType;
import com.edda21.llm.domain.port.out.QuestionGeneratorClient;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP API for generating questions via an LLM-backed generator.
 *
 * <p>This controller exposes a single endpoint:
 *
 * <p>POST {@code /llm/generate} → returns a JSON array of {@link QuestionDTO}.
 *
 * <p>The controller is intentionally thin:
 *
 * <p>• It parses and normalizes request parameters.
 *
 * <p>• It delegates the actual question generation to {@link QuestionGeneratorClient}.
 *
 * <p>• It does not contain any business logic or persistence logic.
 *
 * <p>All framework-specific details (Spring MVC annotations, HTTP mapping, JSON binding) are kept
 * in this adapter layer, while the domain-facing API is the generator port.
 */
@Validated
@RestController
@RequestMapping(path = "/llm", produces = MediaType.APPLICATION_JSON_VALUE)
public class QuestionGenerationController {

  private final QuestionGeneratorClient generator;

  /**
   * Creates a new controller that delegates to the given question generator.
   *
   * @param generator abstraction over the concrete generation engine (LLM, rules, external
   *     service). The controller does not know how questions are produced, it only calls this port.
   */
  public QuestionGenerationController(QuestionGeneratorClient generator) {
    this.generator = generator;
  }

  /**
   * Generates a list of questions based on the parameters provided in the request body.
   *
   * <p>Endpoint: {@code POST /llm/generate}
   *
   * <p>Expected JSON request body (all fields optional, defaults applied if missing):
   *
   * <p>• {@code subject} – high-level subject, e.g. "MATH", "BIOLOGY" (default: {@code "MATH"}).
   *
   * <p>• {@code topic} – more specific topic, e.g. "fractions", "ecosystems" (default: {@code
   * "general"}).
   *
   * <p>• {@code difficulty} – difficulty label, e.g. "A1", "B1", "hard" (default: {@code "B1"}).
   *
   * <p>• {@code type} – question type, e.g. "OPEN", "SINGLE_CHOICE" (default: {@code "OPEN"}).
   *
   * <p>• {@code count} – number of questions to generate (default: {@code 3}).
   *
   * <p>• {@code locale} – locale / language code, e.g. "en", "en-GB", "ru-RU" (default: {@code
   * "en"}).
   *
   * <p>All values are read from a generic {@code Map<String, Object>} to keep the endpoint flexible
   * for prototyping. They are converted to strings and primitive types using {@link
   * String#valueOf(Object)} and {@link Integer#parseInt(String)}.
   *
   * <p>The {@code type} field is converted to a {@link QuestionType} via a helper method {@code
   * fromJson(String)}, which is expected to map string values like "OPEN" or "MULTIPLE_CHOICE" to
   * the corresponding enum constant.
   *
   * <p>On success, the method returns HTTP 200 OK with a JSON array of {@link QuestionDTO} in the
   * response body.
   *
   * @param req request body parsed as a generic map with string keys; values can be strings,
   *     numbers or other JSON-compatible types. Missing keys are replaced with sensible defaults.
   * @return HTTP 200 response containing the generated questions as JSON.
   */
  @PostMapping("/generate")
  public ResponseEntity<List<QuestionDTO>> generate(@RequestBody Map<String, Object> req) {
    // Extract parameters with defaults. All values are normalized to String first.
    String subject = String.valueOf(req.getOrDefault("subject", "MATH"));
    String topic = String.valueOf(req.getOrDefault("topic", "general"));
    String difficulty = String.valueOf(req.getOrDefault("difficulty", "B1"));
    String qtype = String.valueOf(req.getOrDefault("type", "OPEN"));
    int count = Integer.parseInt(String.valueOf(req.getOrDefault("count", 3)));
    String locale = String.valueOf(req.getOrDefault("locale", "en"));

    // Delegate to the domain-level generator and wrap result into HTTP 200.
    return ResponseEntity.ok(
        generator.generate(subject, topic, difficulty, fromJson(qtype), count, locale));
  }
}
