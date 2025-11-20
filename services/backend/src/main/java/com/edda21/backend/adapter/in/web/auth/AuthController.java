package com.edda21.backend.adapter.in.web.auth;

import com.edda21.backend.domain.user.UserRole;
import com.edda21.backend.adapter.out.security.JwtTokenService;
import com.edda21.backend.app.auth.AuthUser;
import com.edda21.backend.app.auth.UserAuthService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoint.
 * Validates username/password against the database and returns a JWT access token.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UserAuthService userAuthService;
  private final JwtTokenService jwtTokenService;

  public AuthController(UserAuthService userAuthService, JwtTokenService jwtTokenService) {
    this.userAuthService = userAuthService;
    this.jwtTokenService = jwtTokenService;
  }

  /**
   * Authenticates user and returns access token.
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    AuthUser authUser = userAuthService.authenticate(request.getUsername(), request.getPassword());

    UUID instructorId = authUser.getRole() == UserRole.INSTRUCTOR
        ? authUser.getInstructorId()
        : null;

    UUID studentId = authUser.getRole() == UserRole.STUDENT
        ? authUser.getStudentId()
        : null;

    List<String> roles = List.of(authUser.getRole().name());

    String token = jwtTokenService.generateAccessToken(
        authUser.getUserId(),
        instructorId,
        studentId,
        authUser.getUsername(),
        roles
    );

    AuthResponse response = new AuthResponse();
    response.setAccessToken(token);
    response.setRole(authUser.getRole().name());
    return ResponseEntity.ok(response);
  }

  /**
   * Simple login request payload.
   */
  public static class LoginRequest {
    private String username;
    private String password;

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }

  /**
   * Token response payload.
   */
  public static class AuthResponse {
    private String accessToken;
    private String role;

    public String getAccessToken() {
      return accessToken;
    }

    public void setAccessToken(String accessToken) {
      this.accessToken = accessToken;
    }

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }
  }
}
