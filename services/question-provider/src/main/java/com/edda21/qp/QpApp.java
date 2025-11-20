package com.edda21.qp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kafka consumer that resolves generation requests into concrete questions. It can blend questions
 * from a (future) local bank with freshly generated LLM questions.
 */
@SpringBootApplication
public class QpApp {
  public static void main(String[] a) {
    SpringApplication.run(QpApp.class, a);
  }
}
