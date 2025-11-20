package com.edda21.llm.application;

import com.edda21.llm.shared.json.JsonMini;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core generation logic.
 * MOCK mode returns deterministic data; OPENAI mode uses Spring AI ChatClient.
 */
@Service
public class GenerationService {

  private final ChatClient chat;
  private final String mode;
  private final String model;

  public GenerationService(ChatClient.Builder builder,
                           @Value("${LLM_MODE:MOCK}") String mode,
                           @Value("${SPRING_AI_MODEL:gpt-4o-mini}") String model) {
    this.chat = builder.build();
    this.mode = mode;
    this.model = model;
  }

  public List<Map<String, Object>> generate(String subject, int count) {
    // Fast local/dev path
    if (!"OPENAI".equalsIgnoreCase(mode)) {
      List<Map<String, Object>> list = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        list.add(Map.of(
            "source", "LLM",
            "subject", subject,
            "difficulty", "B1",
            "body", "[MOCK] " + subject + " generated " + (i + 1),
            "correct", "42"
        ));
      }
      return list;
    }

    String sys = "You are a question generator for an EdTech platform.";
    String user = """
                Generate JSON array of %d short quiz questions for subject = "%s".
                Each item must be an object:
                {"source":"LLM","subject":"%s","difficulty":"B1","body":"<text>","correct":"<short answer>"}.
                Output ONLY valid JSON array, no markdown, no commentary.
                """.formatted(count, subject, subject);

    try {
      // NOTE: new API in Spring AI 1.x
      var options = OpenAiChatOptions.builder()
          .model(model)
          .build();

      String json = chat
          .prompt()
          .system(s -> s.text(sys))
          .user(u -> u.text(user))
          .options(options)
          .call()
          .content();

      return JsonMini.parseListOfMaps(json);
    } catch (Exception e) {
      // Fallback to deterministic output
      List<Map<String, Object>> list = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        list.add(Map.of(
            "source", "LLM",
            "subject", subject,
            "difficulty", "B1",
            "body", "[FALLBACK] " + subject + " generated " + (i + 1),
            "correct", "42"
        ));
      }
      return list;
    }
  }
}
