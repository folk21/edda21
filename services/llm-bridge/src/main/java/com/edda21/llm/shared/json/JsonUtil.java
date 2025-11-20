package com.edda21.llm.shared.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.util.List;

/** Jackson helper to parse JSON arrays to typed DTOs. */
public class JsonUtil {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static <T> List<T> parseList(String json, Class<T> clazz) throws Exception {
    CollectionType type = MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
    return MAPPER.readValue(json, type);
  }
}
