package com.edda21.backend.app.auth;

import com.edda21.backend.domain.user.UserRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authenticates users against the "user", "instructor" and "student" tables.
 * It:
 * - loads user by username from "user" table
 * - joins instructor table to get instructor id (if applicable)
 * - joins student table to get student id (if applicable)
 * - validates password using PasswordEncoder (BCrypt)
 * - returns AuthUser with role and optional instructor/student ids
 */
@Service
public class UserAuthService {

  private final NamedParameterJdbcTemplate jdbc;
  private final PasswordEncoder passwordEncoder;

  public UserAuthService(NamedParameterJdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
    this.jdbc = jdbc;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Authenticates a user by username and password using the database.
   *
   * @param username raw username provided by client
   * @param password raw password provided by client
   * @return authenticated user descriptor
   * @throws IllegalArgumentException if user is not found, disabled or password is invalid
   */
  public AuthUser authenticate(String username, String password) {
    AuthUserRow row = loadUserRow(username);

    if (!row.enabled) {
      throw new IllegalArgumentException("User account is disabled");
    }

    if (!passwordEncoder.matches(password, row.passwordHash)) {
      throw new IllegalArgumentException("Invalid username or password");
    }

    UserRole role = mapRole(row.role);

    return new AuthUser(
        row.userId,
        row.username,
        role,
        row.enabled,
        row.instructorId,
        row.studentId
    );
  }

  private AuthUserRow loadUserRow(String username) {
    String sql =
        "select u.id as user_id, " +
            "       u.username, " +
            "       u.password_hash, " +
            "       u.role, " +
            "       u.enabled, " +
            "       i.id as instructor_id, " +
            "       s.id as student_id " +
            "from \"user\" u " +
            "left join instructor i on i.user_id = u.id " +
            "left join student s on s.user_id = u.id " +
            "where u.username = :username";

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("username", username);

    try {
      return jdbc.queryForObject(sql, params, new AuthUserRowMapper());
    } catch (EmptyResultDataAccessException ex) {
      throw new IllegalArgumentException("Invalid username or password");
    }
  }

  private UserRole mapRole(String role) {
    if (role == null) {
      throw new IllegalArgumentException("User has no role assigned");
    }
    try {
      return UserRole.valueOf(role.toUpperCase());
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Unsupported user role: " + role, ex);
    }
  }

  /**
   * Internal DTO used only for reading from JDBC.
   */
  private static class AuthUserRow {
    final UUID userId;
    final String username;
    final String passwordHash;
    final String role;
    final boolean enabled;
    final UUID instructorId;
    final UUID studentId;

    private AuthUserRow(
        UUID userId,
        String username,
        String passwordHash,
        String role,
        boolean enabled,
        UUID instructorId,
        UUID studentId
    ) {
      this.userId = userId;
      this.username = username;
      this.passwordHash = passwordHash;
      this.role = role;
      this.enabled = enabled;
      this.instructorId = instructorId;
      this.studentId = studentId;
    }
  }

  private static class AuthUserRowMapper implements RowMapper<AuthUserRow> {
    @Override
    public AuthUserRow mapRow(ResultSet rs, int rowNum) throws SQLException {
      UUID userId = rs.getObject("user_id", UUID.class);
      String username = rs.getString("username");
      String passwordHash = rs.getString("password_hash");
      String role = rs.getString("role");
      boolean enabled = rs.getBoolean("enabled");
      UUID instructorId = rs.getObject("instructor_id", UUID.class);
      UUID studentId = rs.getObject("student_id", UUID.class);
      return new AuthUserRow(userId, username, passwordHash, role, enabled, instructorId, studentId);
    }
  }
}
