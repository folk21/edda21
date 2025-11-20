package com.edda21.qp.application.assignment;

import com.edda21.qp.domain.model.AssignmentWithQuestions;
import com.edda21.qp.domain.port.out.AssignmentReadPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Application service that exposes a use case:
 * "load assignment with its questions".
 */
@Service
public class AssignmentQueryService {

  private final AssignmentReadPort assignmentReadPort;

  public AssignmentQueryService(AssignmentReadPort assignmentReadPort) {
    this.assignmentReadPort = assignmentReadPort;
  }

  public Optional<AssignmentWithQuestions> loadAssignmentWithQuestions(UUID assignmentId) {
    return assignmentReadPort.loadWithQuestions(assignmentId);
  }
}
