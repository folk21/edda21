package com.edda21.backend.app.context;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Provides information about the currently authenticated instructor.
 *
 * This implementation supports two main strategies:
 * 1) JWT-based principal (Spring Security OAuth2 resource server):
 *    - instructorId claim (preferred)
 *    - userId claim (fallback)
 *    - sub claim (fallback, last resort)
 *
 * 2) Custom principal object that implements HasInstructorId interface.
 *
 * If no instructor id can be extracted, an IllegalStateException is thrown.
 */
@Service
public class InstructorContextService {

  /**
   * Returns the id of the current instructor based on the Spring Security context.
   *
   * @return instructor id as UUID
   * @throws IllegalStateException if there is no authenticated user
   *                               or the principal does not contain an instructor id
   */
  public UUID getCurrentInstructorId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("No authenticated user present in security context");
    }

    Object principal = authentication.getPrincipal();

    // Strategy 1: JWT principal (Spring Security OAuth2 resource server)
    if (principal instanceof Jwt jwt) {
      String rawId = extractInstructorIdFromJwt(jwt);
      if (rawId == null || rawId.isBlank()) {
        throw new IllegalStateException(
            "JWT principal does not contain instructor id (claims: instructorId/userId/sub)");
      }
      try {
        return UUID.fromString(rawId);
      } catch (IllegalArgumentException ex) {
        throw new IllegalStateException(
            "JWT instructor id is not a valid UUID: '" + rawId + "'", ex);
      }
    }

    // Strategy 2: custom principal implementing HasInstructorId
    if (principal instanceof HasInstructorId hasInstructorId) {
      UUID id = hasInstructorId.getInstructorId();
      if (id == null) {
        throw new IllegalStateException(
            "Principal implements HasInstructorId but returned null instructor id");
      }
      return id;
    }

    // If you use a custom UserDetails implementation, you can adapt this code here.
    throw new IllegalStateException(
        "Unsupported principal type for instructor id resolution: "
            + principal.getClass().getName());
  }

  /**
   * Extracts instructor id from common JWT claims.
   *
   * Expected claim order:
   * - "instructorId" (preferred, must be UUID string)
   * - "userId"       (fallback, if instructor == user)
   * - "sub"          (fallback, last resort)
   */
  private String extractInstructorIdFromJwt(Jwt jwt) {
    String id = jwt.getClaimAsString("instructorId");
    if (id != null && !id.isBlank()) {
      return id;
    }

    id = jwt.getClaimAsString("userId");
    if (id != null && !id.isBlank()) {
      return id;
    }

    return jwt.getSubject();
  }

  /**
   * Extension point for custom principals.
   * If your Authentication#getPrincipal() implements this interface,
   * the service will use getInstructorId() to resolve the current instructor.
   */
  public interface HasInstructorId {
    UUID getInstructorId();
  }
}
