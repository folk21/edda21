package com.edda21.llm.shared.json;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper over Jackson for parsing a JSON array of flat objects.
 * We intentionally keep it minimal to avoid leaking JSON libs to the rest
 * of the codebase. In production, pass typed DTOs instead of Map.
 */
public class JsonMini {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Parse a JSON array like:
   * [ {"source":"LLM","subject":"MATH","difficulty":"B1","body":"...","correct":"42"}, ... ]
   */
  public static List<Map<String, Object>> parseListOfMaps(String json) throws IOException {
    JavaType type = MAPPER.getTypeFactory()
        .constructCollectionType(List.class, Map.class);
    return MAPPER.readValue(json, type);
  }
}
