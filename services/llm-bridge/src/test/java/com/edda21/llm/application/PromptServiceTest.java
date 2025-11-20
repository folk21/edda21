package com.edda21.llm.application;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PromptServiceTest {

  @Test
  void render_replacesPlaceholders() {
    PromptService ps = new PromptService();
    String tpl = "Hello, {{name}}! {{thing}}";
    String out = ps.render(tpl, Map.of("name", "World", "thing", "OK"));
    assertEquals("Hello, World! OK", out);
  }
}
