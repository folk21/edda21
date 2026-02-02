package com.edda21.testing.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Utility for executing SQL scripts from the classpath in tests.
 *
 * <p>This is intentionally simple and supports:
 * <p>- line comments starting with "--";
 * <p>- splitting statements by semicolon (";");
 * <p>- ignoring empty statements.
 */
public final class SqlScriptUtils {

  private SqlScriptUtils() {
  }

  /**
   * Executes all SQL statements from a classpath resource.
   *
   * <p>The resource is treated as a plain text file; statements are separated by
   * semicolons, line comments starting with "--" are ignored.
   *
   * @param jdbcTemplate      JdbcTemplate to execute SQL against
   * @param classpathLocation resource location, e.g. "db/test-data.sql"
   */
  public static void executeScript(JdbcTemplate jdbcTemplate, String classpathLocation) {
    ClassPathResource resource = new ClassPathResource(classpathLocation);
    try (InputStream is = resource.getInputStream();
         BufferedReader reader =
             new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

      StringBuilder currentStatement = new StringBuilder();
      String line;

      while ((line = reader.readLine()) != null) {
        String trimmed = line.trim();
        if (trimmed.startsWith("--") || trimmed.isEmpty()) {
          continue;
        }
        currentStatement.append(line).append('\n');
        if (trimmed.endsWith(";")) {
          String sql = currentStatement.toString().trim();
          sql = sql.substring(0, sql.length() - 1); // remove trailing ';'
          if (!sql.isBlank()) {
            jdbcTemplate.execute(sql);
          }
          currentStatement.setLength(0);
        }
      }

      String remaining = currentStatement.toString().trim();
      if (!remaining.isBlank()) {
        jdbcTemplate.execute(remaining);
      }

    } catch (IOException e) {
      throw new IllegalStateException("Failed to execute SQL script " + classpathLocation, e);
    }
  }
}
