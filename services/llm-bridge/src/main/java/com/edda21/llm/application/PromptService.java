package com.edda21.llm.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** Loads templates from classpath and performs basic {{var}} replacement. */
@Service
public class PromptService {

  public String load(String path) {
    try {
      var res = new ClassPathResource(path);
      try (var in = res.getInputStream()) {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load: " + path, e);
    }
  }

  public String render(String template, Map<String, Object> ctx) {
    String out = template;
    if (ctx != null) {
      for (var e : ctx.entrySet()) {
        out = out.replace("{{" + e.getKey() + "}}", String.valueOf(e.getValue()));
      }
    }
    return out;
  }
}
