package com.edda21.backend.app.question;

import com.edda21.backend.adapter.in.web.question.QuestionGenerationSessionCreateRequest;
import com.edda21.backend.adapter.in.web.question.QuestionGenerationSessionResponse;
import com.edda21.backend.adapter.out.messaging.kafka.QuestionGenerationRequestPayload;
import com.edda21.backend.adapter.out.messaging.kafka.QuestionsRequestProducer;
import com.edda21.backend.adapter.out.persistence.jpa.QuestionGenerationSessionRepository;
import com.edda21.backend.app.context.InstructorContextService;
import com.edda21.backend.domain.question.QuestionGenerationSession;
import com.edda21.backend.domain.question.QuestionGenerationSessionStatus;
import com.edda21.qp.domain.model.QuestionSourceMode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service that manages question generation sessions:
 * - creates a new session
 * - decides which mode to use (DB_ONLY / DB_THEN_LLM / LLM_ONLY)
 * - triggers DB selection and/or Kafka request for LLM
 * - exposes session status for the UI
 */
@Service
public class QuestionGenerationSessionService {

  private final QuestionGenerationSessionRepository sessionRepository;
  private final QuestionsRequestProducer questionsRequestProducer;
  private final QuestionSelectionService questionSelectionService;
  private final InstructorContextService instructorContextService;
  private final ObjectMapper objectMapper;

  public QuestionGenerationSessionService(
      QuestionGenerationSessionRepository sessionRepository,
      QuestionsRequestProducer questionsRequestProducer,
      QuestionSelectionService questionSelectionService,
      InstructorContextService instructorContextService,
      ObjectMapper objectMapper) {

    this.sessionRepository = sessionRepository;
    this.questionsRequestProducer = questionsRequestProducer;
    this.questionSelectionService = questionSelectionService;
    this.instructorContextService = instructorContextService;
    this.objectMapper = objectMapper;
  }

  /**
   * Creates a new question generation session and starts processing
   * according to the requested mode.
   */
  @Transactional
  public QuestionGenerationSessionResponse createSession(
      QuestionGenerationSessionCreateRequest request) {

    UUID instructorId = instructorContextService.getCurrentInstructorId();

    QuestionGenerationSession session = new QuestionGenerationSession();
    session.setInstructorId(instructorId);
    session.setCourseId(request.getCourseId());
    session.setAssignmentId(request.getAssignmentId());
    session.setRequestedCount(request.getRequestedCount());
    session.setDbSelectedCount(0);
    session.setLlmGeneratedCount(0);
    session.setMode(request.getMode());
    session.setStatus(QuestionGenerationSessionStatus.PENDING);

    if (request.getFilters() != null && !request.getFilters().isEmpty()) {
      session.setFilterJson(serializeFilters(request));
    }

    session = sessionRepository.save(session);

    QuestionSourceMode mode = request.getMode();
    if (mode == QuestionSourceMode.DB_ONLY) {
      processDbOnly(session);
    } else if (mode == QuestionSourceMode.LLM_ONLY) {
      processLlmOnly(session, request);
    } else if (mode == QuestionSourceMode.DB_THEN_LLM) {
      processDbThenLlm(session, request);
    }

    session = sessionRepository.save(session);
    return toResponse(session);
  }

  @Transactional(readOnly = true)
  public QuestionGenerationSessionResponse getSession(UUID sessionId) {
    QuestionGenerationSession session = sessionRepository
        .findById(sessionId)
        .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    return toResponse(session);
  }

  @Transactional(readOnly = true)
  public QuestionGenerationSessionResponse getLatestForAssignment(UUID assignmentId) {
    QuestionGenerationSession session = sessionRepository
        .findFirstByAssignmentIdOrderByCreatedAtDesc(assignmentId)
        .orElseThrow(
            () -> new IllegalArgumentException(
                "No session found for assignment: " + assignmentId));
    return toResponse(session);
  }

  private void processDbOnly(QuestionGenerationSession session) {
    session.setStatus(QuestionGenerationSessionStatus.IN_PROGRESS);

    int selectedFromDb =
        questionSelectionService.selectQuestionsFromDb(
            session.getCourseId(),
            session.getAssignmentId(),
            session.getRequestedCount(),
            session.getFilterJson());

    session.setDbSelectedCount(selectedFromDb);
    session.setStatus(QuestionGenerationSessionStatus.COMPLETED);

    if (selectedFromDb == 0) {
      session.setResultCode("NO_DB_QUESTIONS");
    } else if (selectedFromDb < session.getRequestedCount()) {
      session.setResultCode("OK_PARTIAL_DB_ONLY");
    } else {
      session.setResultCode("OK_FULL");
    }
  }

  private void processLlmOnly(
      QuestionGenerationSession session, QuestionGenerationSessionCreateRequest request) {

    session.setStatus(QuestionGenerationSessionStatus.IN_PROGRESS);

    QuestionGenerationRequestPayload payload = buildPayload(session, request, null);
    questionsRequestProducer.send(payload);
    // Session will be updated to COMPLETED by the LLM result processor once the response arrives.
  }

  private void processDbThenLlm(
      QuestionGenerationSession session, QuestionGenerationSessionCreateRequest request) {

    session.setStatus(QuestionGenerationSessionStatus.IN_PROGRESS);

    int selectedFromDb =
        questionSelectionService.selectQuestionsFromDb(
            session.getCourseId(),
            session.getAssignmentId(),
            session.getRequestedCount(),
            session.getFilterJson());
    session.setDbSelectedCount(selectedFromDb);

    int missing = session.getRequestedCount() - selectedFromDb;
    if (missing <= 0) {
      session.setStatus(QuestionGenerationSessionStatus.COMPLETED);
      session.setResultCode("OK_FULL");
      return;
    }

    QuestionGenerationRequestPayload payload = buildPayload(session, request, missing);
    questionsRequestProducer.send(payload);
    // Session will be completed when LLM result processor updates it with llmGeneratedCount.
  }

  private QuestionGenerationRequestPayload buildPayload(
      QuestionGenerationSession session,
      QuestionGenerationSessionCreateRequest request,
      Integer missingCount) {

    QuestionGenerationRequestPayload payload = new QuestionGenerationRequestPayload();
    payload.setSessionId(session.getId());
    payload.setCourseId(session.getCourseId());
    payload.setAssignmentId(session.getAssignmentId());
    payload.setRequestedCount(request.getRequestedCount());
    payload.setMissingCount(missingCount);
    payload.setMode(request.getMode());
    payload.setFilters(request.getFilters());
    return payload;
  }

  private String serializeFilters(QuestionGenerationSessionCreateRequest request) {
    try {
      return objectMapper.writeValueAsString(request.getFilters());
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize filters to JSON", e);
    }
  }

  private QuestionGenerationSessionResponse toResponse(QuestionGenerationSession session) {
    QuestionGenerationSessionResponse dto = new QuestionGenerationSessionResponse();
    dto.setSessionId(session.getId());
    dto.setInstructorId(session.getInstructorId());
    dto.setCourseId(session.getCourseId());
    dto.setAssignmentId(session.getAssignmentId());
    dto.setRequestedCount(session.getRequestedCount());
    dto.setDbSelectedCount(session.getDbSelectedCount());
    dto.setLlmGeneratedCount(session.getLlmGeneratedCount());
    dto.setMode(session.getMode());
    dto.setStatus(session.getStatus());
    dto.setResultCode(session.getResultCode());
    dto.setErrorMessage(session.getErrorMessage());
    dto.setCreatedAt(session.getCreatedAt());
    dto.setUpdatedAt(session.getUpdatedAt());
    return dto;
  }
}
