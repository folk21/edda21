package com.edda21.llm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LLM bridge that hides provider specifics behind a small HTTP API. MOCK mode (default) returns
 * deterministic data; OPENAI mode calls Spring AI ChatClient.
 */
@SpringBootApplication
public class LlmBridgeApp {
  public static void main(String[] a) {
    SpringApplication.run(LlmBridgeApp.class, a);
  }
}
