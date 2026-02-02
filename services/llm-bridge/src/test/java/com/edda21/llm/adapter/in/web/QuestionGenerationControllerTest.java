package com.edda21.llm.adapter.in.web;

import static com.edda21.llm.domain.model.QuestionType.OPEN;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edda21.llm.domain.model.QuestionDTO;
import com.edda21.llm.domain.port.out.QuestionGeneratorClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web MVC slice tests for {@link QuestionGenerationController}.
 *
 * <p>This test class uses {@link WebMvcTest} together with {@link MockMvc} to exercise the HTTP
 * layer around {@link QuestionGenerationController} without starting the full Spring Boot context.
 *
 * <p>The controller is treated as a black box: requests are sent as real HTTP calls to the
 * `/llm/generate` endpoint, and responses are asserted at the JSON level using {@code jsonPath} and
 * status/Content-Type matchers.
 *
 * <p>The domain-level dependency {@link QuestionGeneratorClient} is replaced with a {@link
 * MockBean}. This allows tests to control the generated questions via Mockito stubbing and to focus
 * only on:
 *
 * <p>- Mapping of the incoming JSON request body to controller arguments.
 *
 * <p>- Correct invocation of the generator with expected parameters.
 *
 * <p>- Mapping of the returned DTOs to the HTTP JSON response produced by the controller.
 */
@WebMvcTest(controllers = QuestionGenerationController.class)
class QuestionGenerationControllerTest {

  @Autowired MockMvc mvc;

  @MockBean QuestionGeneratorClient generator;

  @Test
  void postGenerate_returnsQuestions() throws Exception {
    // given
    var dto =
        new QuestionDTO("LLM", "MATH", "algebra", "B1", OPEN, "What is 2+2?", null, "4", null);

    Mockito.when(
            generator.generate(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.any(),
                Mockito.anyInt(),
                Mockito.anyString()))
        .thenReturn(List.of(dto));

    // when / then
    mvc.perform(
            post("/llm/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"MATH\",\"topic\":\"algebra\",\"count\":1}"))
        .andExpect(status().isOk())
        // Use compatible content type matcher because Spring may append charset to the response.
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].subject").value("MATH"))
        .andExpect(jsonPath("$[0].body").value("What is 2+2?"));

    // Additionally verify that the controller forwarded parameters correctly to the generator.
    verify(generator)
        .generate(
            eq("MATH"), // subject from the request
            eq("algebra"), // topic from the request
            eq("B1"), // default difficulty
            eq(OPEN), // default type -> fromJson("OPEN")
            eq(1), // count from the request
            eq("en")); // default locale
  }
}