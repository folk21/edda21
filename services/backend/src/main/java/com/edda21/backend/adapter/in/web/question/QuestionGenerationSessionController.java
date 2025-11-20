package com.edda21.backend.adapter.in.web.question;

import com.edda21.backend.app.question.QuestionGenerationSessionService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for managing question generation sessions.
 * These endpoints are intended for instructor UI.
 */
@RestController
@RequestMapping("/question-sessions")
public class QuestionGenerationSessionController {

  private final QuestionGenerationSessionService sessionService;

  public QuestionGenerationSessionController(QuestionGenerationSessionService sessionService) {
    this.sessionService = sessionService;
  }

  @PreAuthorize("hasRole('INSTRUCTOR')")
  @PostMapping
  public ResponseEntity<QuestionGenerationSessionResponse> createSession(
      @RequestBody QuestionGenerationSessionCreateRequest request) {

    QuestionGenerationSessionResponse response = sessionService.createSession(request);
    return ResponseEntity.ok(response);
  }

  @PreAuthorize("hasRole('INSTRUCTOR')")
  @GetMapping("/{sessionId}")
  public ResponseEntity<QuestionGenerationSessionResponse> getSession(
      @PathVariable("sessionId") UUID sessionId) {

    QuestionGenerationSessionResponse response = sessionService.getSession(sessionId);
    return ResponseEntity.ok(response);
  }

  @PreAuthorize("hasRole('INSTRUCTOR')")
  @GetMapping("/by-assignment/{assignmentId}")
  public ResponseEntity<QuestionGenerationSessionResponse> getLatestSessionForAssignment(
      @PathVariable("assignmentId") UUID assignmentId) {

    QuestionGenerationSessionResponse response =
        sessionService.getLatestForAssignment(assignmentId);
    return ResponseEntity.ok(response);
  }
}
