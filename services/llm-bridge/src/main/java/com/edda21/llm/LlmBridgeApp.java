package com.edda21.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LLM bridge that hides provider specifics behind a small HTTP API.
 *
 * <p>Generation strategy is selected via {@code generator.mode} (or {@code GENERATOR_MODE} env):
 *
 * <p>- {@code stub} (default): deterministic questions for local development and tests.
 *
 * <p>- {@code llm}: calls a real LLM via Spring AI, behind the {@code ChatExecutor} abstraction.
 */
@SpringBootApplication
public class LlmBridgeApp {
  public static void main(String[] a) {
    SpringApplication.run(LlmBridgeApp.class, a);
  }
}
