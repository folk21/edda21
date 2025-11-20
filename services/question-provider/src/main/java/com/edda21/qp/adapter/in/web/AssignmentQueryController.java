package com.edda21.qp.adapter.in.web;

import com.edda21.qp.application.assignment.AssignmentQueryService;
import com.edda21.qp.domain.model.AssignmentWithQuestions;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoint to fetch an assignment together with its questions.
 *
 * Instructors can call this endpoint after generation is finished
 * to inspect the assignment content.
 */
@RestController
@RequestMapping("/assignments")
public class AssignmentQueryController {

  private final AssignmentQueryService assignmentQueryService;

  public AssignmentQueryController(AssignmentQueryService assignmentQueryService) {
    this.assignmentQueryService = assignmentQueryService;
  }

  @GetMapping("/{assignmentId}")
  public ResponseEntity<AssignmentWithQuestions> getAssignmentWithQuestions(
      @PathVariable UUID assignmentId) {

    Optional<AssignmentWithQuestions> result =
        assignmentQueryService.loadAssignmentWithQuestions(assignmentId);

    return result
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
