package com.edda21.qp.adapter.in.messaging.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.edda21.qp.application.llm.LlmResultProcessorService;

/**
 * Kafka listener that consumes LLM responses from the "questions.response" topic
 * and delegates processing to LlmResultProcessorService.
 */
@Component
public class LlmResponseKafkaListener {

  private static final Logger log = LoggerFactory.getLogger(LlmResponseKafkaListener.class);

  private final LlmResultProcessorService processorService;

  public LlmResponseKafkaListener(LlmResultProcessorService processorService) {
    this.processorService = processorService;
  }

  /**
   * Consumes messages from the response topic.
   *
   * Topic name can be configured via property:
   *   questions.response.topic
   * Default: "questions.response"
   *
   * Group id is fixed to "llm-result-processor" so that this service
   * consumes each response exactly once in this group.
   */
  @KafkaListener(
      topics = "#{'${questions.response.topic:questions.response}'}",
      groupId = "llm-result-processor"
  )
  public void onMessage(ConsumerRecord<String, LlmQuestionsResponsePayload> record) {
    LlmQuestionsResponsePayload payload = record.value();
    try {
      processorService.processResponse(payload);
    } catch (Exception ex) {
      log.error(
          "Failed to process LLM response for key={}, sessionId={}",
          record.key(),
          payload != null ? payload.getSessionId() : null,
          ex
      );
      // Depending on your retry strategy, you may rethrow, send to DLQ, etc.
      // For now we just log the error.
    }
  }
}
