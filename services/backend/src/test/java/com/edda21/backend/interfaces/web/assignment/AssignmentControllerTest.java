package com.edda21.backend.interfaces.web.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edda21.backend.adapter.in.web.assignment.AssignmentController;
import com.edda21.backend.adapter.out.http.QuestionProviderClient;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AssignmentControllerTest {

  @Test
  void generate_publishesToKafka() throws Exception {
    @SuppressWarnings("unchecked")
    KafkaTemplate<String, Object> kafka = Mockito.mock(KafkaTemplate.class);
    QuestionProviderClient questionProviderClient = Mockito.mock(QuestionProviderClient.class);
    var controller = new AssignmentController(kafka, questionProviderClient);
    MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

    mvc.perform(
            post("/assignments/1111-2222-3333-4444/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"MATH\",\"count\":2}"))
        .andExpect(status().isAccepted());

    ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
    Mockito.verify(kafka).send(Mockito.anyString(), Mockito.anyString(), payload.capture());
    assertThat(payload.getValue()).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) payload.getValue();
    assertThat(body).containsEntry("subject", "MATH");
    assertThat(body).containsEntry("count", 2);
    assertThat(body).containsKey("assignmentId");
    assertThat(body).containsKey("requestId");
  }
}
