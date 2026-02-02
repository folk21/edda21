package com.edda21.testing.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;

/**
 * Small JSON helper for tests.
 *
 * <p>Provides convenient methods for reading JSON resources from the classpath
 * using a shared ObjectMapper instance.
 */
public final class TestJsonUtils {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private TestJsonUtils() {
  }

  /**
   * Reads JSON from the given classpath location into the provided target type.
   *
   * @param classpathLocation resource location, e.g. "db/questions-full.json"
   * @param typeReference     Jackson TypeReference describing the target type
   * @param <T>               target type
   * @return deserialized object
   */
  public static <T> T readFromClasspath(
      String classpathLocation,
      TypeReference<T> typeReference) {

    ClassPathResource resource = new ClassPathResource(classpathLocation);
    try (InputStream is = resource.getInputStream()) {
      return OBJECT_MAPPER.readValue(is, typeReference);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load JSON from " + classpathLocation, e);
    }
  }
}
