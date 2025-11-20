package com.edda21.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Backend service exposes instructor-facing REST API. It publishes "generate questions" requests to
 * Kafka and returns 202 Accepted immediately. This keeps the HTTP path fast and uses Kafka for
 * asynchronous processing.
 */
@SpringBootApplication
public class BackendApp {
  public static void main(String[] args) {
    SpringApplication.run(BackendApp.class, args);
  }
}
