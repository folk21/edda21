package com.edda21.qp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;

import com.edda21.qp.adapter.out.persistence.QuestionRepo;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class QuestionRepoTest {

  @Test
  void saveQuestions_insertsEachQuestionAndLink() {
    NamedParameterJdbcTemplate jdbc = Mockito.mock(NamedParameterJdbcTemplate.class);
    QuestionRepo repo = new QuestionRepo(jdbc);

    UUID assignmentId = UUID.randomUUID();
    List<Map<String, Object>> qs =
        List.of(
            Map.of(
                "source",
                "LLM",
                "subject",
                "MATH",
                "difficulty",
                "B1",
                "body",
                "Q1",
                "correct",
                "4"),
            Map.of(
                "source",
                "LLM",
                "subject",
                "MATH",
                "difficulty",
                "B1",
                "body",
                "Q2",
                "correct",
                "4"));

    repo.saveQuestions(assignmentId, qs);

    ArgumentCaptor<MapSqlParameterSource> cap =
        ArgumentCaptor.forClass(MapSqlParameterSource.class);
    Mockito.verify(jdbc, times(2))
        .update(Mockito.startsWith("insert into question"), cap.capture());
    Mockito.verify(jdbc, times(2))
        .update(
            Mockito.startsWith("insert into assignment_question"),
            Mockito.any(MapSqlParameterSource.class));

    var params = cap.getAllValues();
    assertThat(params.get(0).getValues()).containsKeys("id", "src", "sub", "diff", "body", "corr");
  }
}
