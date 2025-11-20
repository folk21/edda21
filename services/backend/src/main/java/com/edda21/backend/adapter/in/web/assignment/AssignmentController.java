package com.edda21.backend.adapter.in.web.assignment;

import com.edda21.backend.adapter.out.http.QuestionProviderClient;
import com.edda21.qp.domain.model.AssignmentWithQuestions;
import java.util.*;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint to request question generation for an assignment. It emits a Kafka message
 * containing assignment id and parameters. Clients can poll results in DB or listen on a
 * 'completed' topic in future versions.
 */
@RestController
@RequestMapping("/assignments")
public class AssignmentController {

  private final KafkaTemplate<String, Object> kafka;
  private final QuestionProviderClient questionProviderClient;

  public AssignmentController(
      KafkaTemplate<String, Object> kafka, QuestionProviderClient questionProviderClient) {
    this.kafka = kafka;
    this.questionProviderClient = questionProviderClient;
  }

  /** Read assignment with all its questions. */
  @PreAuthorize("hasRole('INSTRUCTOR')")
  @GetMapping("/{assignmentId}")
  public ResponseEntity<AssignmentWithQuestions> get(@PathVariable String assignmentId) {
    UUID id;
    try {
      id = UUID.fromString(assignmentId);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().build();
    }

    Optional<AssignmentWithQuestions> result =
        questionProviderClient.getAssignmentWithQuestions(id);

    return result.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PreAuthorize("hasRole('INSTRUCTOR')")
  @PostMapping("/{assignmentId}/generate")
  public ResponseEntity<?> generate(
      @PathVariable String assignmentId, @RequestBody Map<String, Object> body) {

    String reqId = UUID.randomUUID().toString();
    body.put("requestId", reqId);
    body.put("assignmentId", assignmentId);

    // sourceMode: строка, по умолчанию DB_THEN_LLM
    String modeRaw = String.valueOf(body.getOrDefault("sourceMode", "DB_THEN_LLM"));
    body.put("sourceMode", modeRaw);

    kafka.send(
        System.getenv().getOrDefault("TOPIC_REQUEST", "questions.request"), assignmentId, body);

    return ResponseEntity.accepted().body(Map.of("requestId", reqId, "status", "ENQUEUED"));
  }
}
