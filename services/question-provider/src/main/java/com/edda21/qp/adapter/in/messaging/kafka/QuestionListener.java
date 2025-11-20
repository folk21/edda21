package com.edda21.qp.adapter.in.messaging.kafka;

import com.edda21.qp.adapter.out.persistence.QuestionRepo;
import com.edda21.qp.domain.model.Question;
import com.edda21.qp.domain.model.QuestionSourceMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class QuestionListener {

  private final QuestionRepo questionRepo;
  private final RestTemplate restTemplate;

  public QuestionListener(
      QuestionRepo questionRepo,
      RestTemplateBuilder restTemplateBuilder,
      @Value("${llm.base-url:http://localhost:8082}") String llmBaseUrl) {

    this.questionRepo = questionRepo;
    this.restTemplate = restTemplateBuilder.rootUri(llmBaseUrl).build();
  }

  /**
   * Kafka listener for question-generation requests.
   *
   * <p>Example payload: { "assignmentId": "1111-2222-3333-4444", "subject": "MATH", "count": 10,
   * "sourceMode": "DB_THEN_LLM" }
   */
  @KafkaListener(topics = "${topic.request:questions.request}", groupId = "question-provider")
  public void onMessage(Map<String, Object> payload, Acknowledgment ack) {
    log.info("Received question-generation request: {}", payload);
    try {
      handle(payload);
    } catch (Exception e) {
      log.error("Failed to process question-generation request: {}", payload, e);
      // Consider adding dedicated error handling / alerting here if needed.
    } finally {
      // Simple strategy: always acknowledge, no retry logic for now.
      ack.acknowledge();
    }
  }

  private void handle(Map<String, Object> payload) {
    UUID assignmentId = UUID.fromString(String.valueOf(payload.get("assignmentId")));
    String subject = String.valueOf(payload.getOrDefault("subject", "MATH"));
    int count = Integer.parseInt(String.valueOf(payload.getOrDefault("count", 3)));

    String rawMode =
        String.valueOf(payload.getOrDefault("sourceMode", QuestionSourceMode.DB_THEN_LLM.name()));
    QuestionSourceMode mode = QuestionSourceMode.valueOf(rawMode);

    log.info(
        "Generating questions for assignment {}, subject={}, count={}, mode={}",
        assignmentId,
        subject,
        count,
        mode);

    switch (mode) {
      case DB_ONLY -> handleDbOnly(assignmentId, subject, count);
      case DB_THEN_LLM -> handleDbThenLlm(assignmentId, subject, count);
      case LLM_ONLY -> handleLlmOnly(assignmentId, subject, count);
    }
  }

  /**
   * DB_ONLY: - Try to load all requested questions from the database. - If there are not enough
   * questions, do NOT save anything for this assignment, just log the issue (and optionally persist
   * it to a session table).
   */
  private void handleDbOnly(UUID assignmentId, String subject, int requestedCount) {
    List<Question> dbQuestions = questionRepo.loadFromDb(subject, requestedCount);

    if (dbQuestions.size() < requestedCount) {
      log.warn(
          "DB_ONLY: not enough questions in DB for assignment {} (subject={}, requested={}, found={})",
          assignmentId,
          subject,
          requestedCount,
          dbQuestions.size());

      // TODO: store this error in a session table if needed
      // sessionRepo.saveError(...);

      // Do not save anything to assignment_question in DB_ONLY mode when the DB is insufficient.
      return;
    }

    questionRepo.saveQuestionsForAssignment(assignmentId, dbQuestions);
  }

  /**
   * DB_THEN_LLM: - Load as many questions as possible from the database. - If the DB does not
   * contain enough questions, request the missing number from LLM. - Save all questions (DB + LLM)
   * for the assignment.
   */
  private void handleDbThenLlm(UUID assignmentId, String subject, int requestedCount) {
    List<Question> dbQuestions = questionRepo.loadFromDb(subject, requestedCount);
    int fromDb = dbQuestions.size();
    int needFromLlm = requestedCount - fromDb;

    List<Question> result = new ArrayList<>(dbQuestions);

    if (needFromLlm > 0) {
      log.info(
          "DB_THEN_LLM: found {} questions in DB, requesting {} more from LLM for assignment {}",
          fromDb,
          needFromLlm,
          assignmentId);

      List<Question> llmQuestions = callLlm(subject, needFromLlm);
      result.addAll(llmQuestions);
    }

    if (result.isEmpty()) {
      log.warn(
          "DB_THEN_LLM: no questions generated at all for assignment {} (subject={})",
          assignmentId,
          subject);
      // Optionally store an error in a session table here.
      return;
    }

    questionRepo.saveQuestionsForAssignment(assignmentId, result);
  }

  /**
   * LLM_ONLY: - Ignore the database completely. - Fetch all requested questions from the LLM
   * service. - Save them for the assignment.
   */
  private void handleLlmOnly(UUID assignmentId, String subject, int requestedCount) {
    List<Question> llmQuestions = callLlm(subject, requestedCount);

    if (llmQuestions.isEmpty()) {
      log.warn(
          "LLM_ONLY: LLM returned no questions for assignment {} (subject={}, count={})",
          assignmentId,
          subject,
          requestedCount);
      // Optionally store an error in a session table here.
      return;
    }

    questionRepo.saveQuestionsForAssignment(assignmentId, llmQuestions);
  }

  /**
   * Calls the LLM service and maps the response into a list of {@link Question} objects. The LLM is
   * expected to return a List<Map>, where at least the "body" field is present.
   */
  @SuppressWarnings("unchecked")
  private List<Question> callLlm(String subject, int count) {
    if (count <= 0) {
      return List.of();
    }

    Map<String, Object> requestBody =
        Map.of(
            "subject", subject,
            "count", count);

    List<Map<String, Object>> response =
        restTemplate.postForObject(
            "/llm/generate", // root URI is configured via RestTemplateBuilder
            requestBody,
            List.class);

    if (response == null || response.isEmpty()) {
      return List.of();
    }

    List<Question> result = new ArrayList<>();
    for (Map<String, Object> q : response) {
      Object body = q.get("body");
      if (body == null) {
        // Skip malformed entries without a question body.
        continue;
      }

      result.add(
          new Question(
              null, // ID will be generated later in QuestionRepo.saveQuestions
              String.valueOf(q.getOrDefault("source", "LLM")),
              String.valueOf(q.getOrDefault("subject", subject)),
              String.valueOf(q.getOrDefault("difficulty", "B1")),
              String.valueOf(body),
              String.valueOf(q.getOrDefault("correct", ""))));
    }

    return result;
  }
}
