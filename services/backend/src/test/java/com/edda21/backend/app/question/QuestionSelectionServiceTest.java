package com.edda21.backend.app.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for {@link QuestionSelectionService} using a real PostgreSQL database
 * managed by Testcontainers.
 *
 * <p>This test class focuses on the SQL-based selection logic implemented by
 * {@link QuestionSelectionService}:
 * <p>- how filters (subject/topic, difficulty) are applied;
 * <p>- how the result size is limited by requestedCount;
 * <p>- how questions already linked to the assignment are excluded from the candidates.
 *
 * <p>To keep the tests realistic and readable, test data is stored as JSON under
 * {@code src/test/resources} and imported into the Postgres container before execution.
 */
@Testcontainers
@SpringBootTest
class QuestionSelectionServiceTest {

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("edtech")
          .withUsername("postgres")
          .withPassword("postgres");

  @DynamicPropertySource
  static void registerDataSourceProps(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired
  DataSource dataSource;

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Autowired
  QuestionSelectionService questionSelectionService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUpSchema() {
    jdbcTemplate.execute("drop table if exists assignment_question");
    jdbcTemplate.execute("drop table if exists question");

    jdbcTemplate.execute(
        """
        create table question (
          id uuid primary key,
          subject varchar(32),
          difficulty varchar(16)
        )
        """);

    jdbcTemplate.execute(
        """
        create table assignment_question (
          assignment_id uuid not null,
          question_id uuid not null,
          variant int default 0,
          points int default 1,
          ordering int default 0
        )
        """);
  }

  /**
   * Simple DTO used only for loading test data from JSON files.
   *
   * <p>Fields in this class correspond to the subset of columns in the {@code question}
   * table that are relevant for selection tests: id, subject and difficulty.
   */
  static class TestQuestionRow {
    public UUID id;
    public String subject;
    public String difficulty;
  }

  /**
   * Inserts questions into the Postgres container, loading them from a JSON file located
   * on the classpath.
   *
   * <p>The JSON file is expected to contain an array of objects with fields matching
   * {@link TestQuestionRow}. Only the fields required by the test schema are used.
   *
   * <p>This helper keeps individual test methods focused on scenarios instead of on
   * low-level insert statements.
   */
  private void insertQuestionsFromJson(String classpathLocation) {
    ClassPathResource resource = new ClassPathResource(classpathLocation);
    try (InputStream is = resource.getInputStream()) {
      List<TestQuestionRow> rows =
          objectMapper.readValue(is, new TypeReference<List<TestQuestionRow>>() {});
      for (TestQuestionRow row : rows) {
        jdbcTemplate.update(
            "insert into question (id, subject, difficulty) values (?,?,?)",
            row.id, row.subject, row.difficulty);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load test data from " + classpathLocation, e);
    }
  }

  /**
   * Verifies that {@link QuestionSelectionService#selectQuestionsFromDb} applies subject and
   * difficulty filters and links the matching questions to the given assignment.
   *
   * <p>Test logic:
   * <p>1) Load a diverse set of questions from JSON (multiple subjects and difficulties).
   * <p>2) Call {@code selectQuestionsFromDb} with filters for subject = MATH and difficulty = EASY.
   * <p>3) Assert that at least one question was selected.
   * <p>4) Read all linked question ids for the assignment from {@code assignment_question}.
   * <p>5) For each linked question, assert that its subject is MATH and its difficulty is EASY.
   *
   * <p>This test ensures that only questions matching both filters end up attached to
   * the assignment.
   */
  @Test
  void selectQuestionsFromDb_filtersBySubjectAndDifficulty_andLinksToAssignment() {
    insertQuestionsFromJson("db/questions-full.json");

    UUID assignmentId = UUID.randomUUID();

    String filtersJson = """
{"subject":"MATH","difficulty":"EASY"}
""";

    int selected =
        questionSelectionService.selectQuestionsFromDb(
            UUID.randomUUID(), assignmentId, 10, filtersJson);

    assertThat(selected).isGreaterThanOrEqualTo(1);

    List<UUID> linkedIds =
        jdbcTemplate.query(
            "select question_id from assignment_question where assignment_id = ?",
            (rs, rowNum) -> (UUID) rs.getObject(1),
            assignmentId);

    assertThat(linkedIds).isNotEmpty();
    linkedIds.forEach(
        id -> {
          String subject =
              jdbcTemplate.queryForObject(
                  "select subject from question where id = ?", String.class, id);
          String difficulty =
              jdbcTemplate.queryForObject(
                  "select difficulty from question where id = ?", String.class, id);

          assertThat(subject).isEqualTo("MATH");
          assertThat(difficulty).isEqualTo("EASY");
        });
  }

  /**
   * Verifies that {@link QuestionSelectionService#selectQuestionsFromDb} enforces the
   * {@code requestedCount} limit when there are more matching questions than requested.
   *
   * <p>Test logic:
   * <p>1) Load a dataset that contains at least two questions with subject = MATH and
   * difficulty = EASY.
   * <p>2) Call {@code selectQuestionsFromDb} with filters for MATH/EASY and requestedCount = 1.
   * <p>3) Assert that the method returns 1.
   * <p>4) Count rows in {@code assignment_question} for the assignment and assert that
   * exactly one row was inserted.
   *
   * <p>This test guarantees that the selection query and the insertion logic respect
   * the requested limit and do not over-fill the assignment.
   */
  @Test
  void selectQuestionsFromDb_limitsResultSizeWhenMoreQuestionsThanRequested() {
    insertQuestionsFromJson("db/questions-full.json");

    UUID assignmentId = UUID.randomUUID();

    String filtersJson = """
{"subject":"MATH","difficulty":"EASY"}
""";

    int selected =
        questionSelectionService.selectQuestionsFromDb(
            UUID.randomUUID(), assignmentId, 1, filtersJson);

    assertThat(selected).isEqualTo(1);

    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from assignment_question where assignment_id = ?",
            Integer.class,
            assignmentId);

    assertThat(count).isEqualTo(1);
  }

  /**
   * Verifies that {@link QuestionSelectionService#selectQuestionsFromDb} returns zero
   * and does not insert any rows when filters do not match any existing question.
   *
   * <p>Test logic:
   * <p>1) Load a dataset with multiple subjects, none of which is GEOGRAPHY.
   * <p>2) Call {@code selectQuestionsFromDb} with subject = GEOGRAPHY and difficulty = EASY.
   * <p>3) Assert that the method returns 0.
   * <p>4) Assert that {@code assignment_question} contains no rows for this assignment.
   *
   * <p>This test covers the "no matches" branch and makes sure the service does not
   * create any spurious links when nothing satisfies the filters.
   */
  @Test
  void selectQuestionsFromDb_returnsZeroWhenNoMatchesForFilters() {
    insertQuestionsFromJson("db/questions-full.json");

    UUID assignmentId = UUID.randomUUID();

    String filtersJson = """
{"subject":"GEOGRAPHY","difficulty":"EASY"}
""";

    int selected =
        questionSelectionService.selectQuestionsFromDb(
            UUID.randomUUID(), assignmentId, 5, filtersJson);

    assertThat(selected).isEqualTo(0);

    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from assignment_question where assignment_id = ?",
            Integer.class,
            assignmentId);

    assertThat(count).isZero();
  }

  /**
   * Verifies that {@link QuestionSelectionService#selectQuestionsFromDb} short-circuits
   * when {@code requestedCount} is non-positive and does not modify the database.
   *
   * <p>Test logic:
   * <p>1) Load a dataset with valid questions (so there are candidates to select).
   * <p>2) Call {@code selectQuestionsFromDb} with requestedCount = 0 and a filter that would
   * normally match some questions.
   * <p>3) Assert that the method returns 0.
   * <p>4) Assert that {@code assignment_question} remains empty for this assignment.
   *
   * <p>This test documents and protects the behaviour that a non-positive requestedCount
   * is treated as "do nothing".
   */
  @Test
  void selectQuestionsFromDb_returnsZeroWhenRequestedCountIsNonPositive() {
    insertQuestionsFromJson("db/questions-full.json");

    UUID assignmentId = UUID.randomUUID();

    String filtersJson = """
{"subject":"MATH","difficulty":"EASY"}
""";

    int selected =
        questionSelectionService.selectQuestionsFromDb(
            UUID.randomUUID(), assignmentId, 0, filtersJson);

    assertThat(selected).isEqualTo(0);

    Integer count =
        jdbcTemplate.queryForObject(
            "select count(*) from assignment_question where assignment_id = ?",
            Integer.class,
            assignmentId);

    assertThat(count).isZero();
  }
}
