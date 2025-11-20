package com.edda21.backend.app.context;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Provides information about the currently authenticated student.
 *
 * This implementation expects that JWT tokens contain a "studentId" claim
 * with a UUID string value. It also supports a custom principal that implements
 * HasStudentId interface.
 */
@Service
public class StudentContextService {

  /**
   * Returns the id of the current student based on the Spring Security context.
   *
   * @return student id as UUID
   * @throws IllegalStateException if there is no authenticated user
   *                               or the principal does not contain a student id
   */
  public UUID getCurrentStudentId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("No authenticated user present in security context");
    }

    Object principal = authentication.getPrincipal();

    // Strategy 1: JWT principal (Spring Security OAuth2 resource server)
    if (principal instanceof Jwt jwt) {
      String rawId = jwt.getClaimAsString("studentId");
      if (rawId == null || rawId.isBlank()) {
        throw new IllegalStateException(
            "JWT principal does not contain student id claim 'studentId'");
      }
      try {
        return UUID.fromString(rawId);
      } catch (IllegalArgumentException ex) {
        throw new IllegalStateException(
            "JWT student id is not a valid UUID: '" + rawId + "'", ex);
      }
    }

    // Strategy 2: custom principal implementing HasStudentId
    if (principal instanceof HasStudentId hasStudentId) {
      UUID id = hasStudentId.getStudentId();
      if (id == null) {
        throw new IllegalStateException(
            "Principal implements HasStudentId but returned null student id");
      }
      return id;
    }

    throw new IllegalStateException(
        "Unsupported principal type for student id resolution: "
            + principal.getClass().getName());
  }

  /**
   * Extension point for custom principals.
   * If your Authentication#getPrincipal() implements this interface,
   * the service will use getStudentId() to resolve the current student.
   */
  public interface HasStudentId {
    UUID getStudentId();
  }
}
