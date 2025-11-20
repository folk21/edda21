package com.edda21.backend.adapter.in.web.assignment;

import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * REST endpoint to request question generation for an assignment. It emits a Kafka message
 * containing assignment id and parameters. Clients can poll results in DB or listen on a
 * 'completed' topic in future versions.
 */
@RestController
@RequestMapping("/assignments")
public class AssignmentController {

  private final KafkaTemplate<String, Object> kafka;

  public AssignmentController(KafkaTemplate<String, Object> kafka) {
    this.kafka = kafka;
  }

  @PreAuthorize("hasRole('INSTRUCTOR')")
  @PostMapping("/{assignmentId}/generate")
  public ResponseEntity<?> generate(
      @PathVariable String assignmentId, @RequestBody Map<String, Object> body) {
    String reqId = UUID.randomUUID().toString();
    body.put("requestId", reqId);
    body.put("assignmentId", assignmentId);
    kafka.send(
        System.getenv().getOrDefault("TOPIC_REQUEST", "questions.request"), assignmentId, body);
    return ResponseEntity.accepted().body(Map.of("requestId", reqId, "status", "ENQUEUED"));
  }
}
