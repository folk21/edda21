package com.edda21.qp.adapter.in.messaging.kafka;

import java.util.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.*;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.*;

/**
 * Manual-ack consumer with small concurrency. We keep auto-commit disabled to avoid losing messages
 * on failures.
 */
@EnableKafka
@Configuration
public class KafkaConsumerCfg {
  @Bean
  public ConsumerFactory<String, Map> consumerFactory() {
    Map<String, Object> p = new HashMap<>();
    p.put(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
        System.getenv().getOrDefault("KAFKA_BOOTSTRAP", "localhost:9092"));
    p.put(
        ConsumerConfig.GROUP_ID_CONFIG,
        System.getenv().getOrDefault("KAFKA_GROUP_ID", "questions-provider-group"));
    p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    p.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    return new DefaultKafkaConsumerFactory<>(
        p,
        new StringDeserializer(),
        new ErrorHandlingDeserializer<>(new JsonDeserializer<>(Map.class, false)));
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, Map> kafkaListenerContainerFactory() {
    var f = new ConcurrentKafkaListenerContainerFactory<String, Map>();
    f.setConsumerFactory(consumerFactory());
    f.setConcurrency(2);
    return f;
  }
}
