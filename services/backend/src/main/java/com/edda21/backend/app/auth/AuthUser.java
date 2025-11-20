package com.edda21.backend.app.auth;

import com.edda21.backend.domain.user.UserRole;
import java.util.UUID;

/**
 * Represents a user authenticated by username/password against the database.
 */
public class AuthUser {

  private final UUID userId;
  private final String username;
  private final UserRole role;
  private final boolean enabled;
  private final UUID instructorId;
  private final UUID studentId;

  public AuthUser(
      UUID userId,
      String username,
      UserRole role,
      boolean enabled,
      UUID instructorId,
      UUID studentId
  ) {
    this.userId = userId;
    this.username = username;
    this.role = role;
    this.enabled = enabled;
    this.instructorId = instructorId;
    this.studentId = studentId;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }

  public UserRole getRole() {
    return role;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public UUID getInstructorId() {
    return instructorId;
  }

  public UUID getStudentId() {
    return studentId;
  }
}
