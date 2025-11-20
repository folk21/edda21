package com.edda21.qp.adapter.in.messaging.kafka;

import com.edda21.qp.domain.port.out.QuestionWritePort;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.*;

/** Listener pulls part from BANK and the rest from LLM bridge, then persists. */
@Component
public class QuestionListener {
  private final QuestionWritePort repo;
  private final RestClient llm;

  public QuestionListener(QuestionWritePort repo) {
    this.repo = repo;
    this.llm =
        RestClient.builder()
            .baseUrl(System.getenv().getOrDefault("LLM_BRIDGE_URL", "http://localhost:8098"))
            .build();
  }

  @KafkaListener(
      topics = "#{environment.TOPIC_REQUEST ?: 'questions.request'}",
      containerFactory = "kafkaListenerContainerFactory")
  public void onMessage(java.util.Map payload, Acknowledgment ack) {
    try {
      var assignmentId = java.util.UUID.fromString(String.valueOf(payload.get("assignmentId")));
      String subject = String.valueOf(payload.getOrDefault("subject", "MATH"));
      int count = Integer.parseInt(String.valueOf(payload.getOrDefault("count", 3)));
      java.util.List<java.util.Map<String, Object>> out = new java.util.ArrayList<>();
      int fromBank = Math.min(2, count);
      for (int i = 0; i < fromBank; i++) {
        out.add(
            java.util.Map.of(
                "source",
                "BANK",
                "subject",
                subject,
                "difficulty",
                "B1",
                "body",
                "[BANK] " + subject + " demo " + (i + 1),
                "correct",
                "42"));
      }
      count -= fromBank;
      if (count > 0) {
        ResponseEntity<java.util.List> r =
            llm.post()
                .uri("/llm/generate")
                .body(java.util.Map.of("subject", subject, "count", count))
                .retrieve()
                .toEntity(java.util.List.class);
        if (r.getBody() != null) {
          for (Object o : r.getBody()) if (o instanceof java.util.Map m) out.add(m);
        }
      }
      repo.saveQuestions(assignmentId, out);
      ack.acknowledge();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
