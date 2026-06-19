package com.loadup.assessment.order.config;

import com.loadup.assessment.contracts.OrderEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    @Bean
    KafkaTemplate<String, OrderEvent> orderKafkaTemplate(final KafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>(properties.buildProducerProperties(null));
        configs.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(configs));
    }

    @Bean
    KafkaAdmin.NewTopics orderTopics() {
        return new KafkaAdmin.NewTopics(new NewTopic("orders.events.v1", 1, (short) 1));
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, OrderEvent> orderKafkaListenerContainerFactory(final KafkaProperties properties) {
        Map<String, Object> configs = new HashMap<>(properties.buildConsumerProperties(null));
        configs.put(JsonDeserializer.TRUSTED_PACKAGES, "com.loadup.assessment.contracts");
        configs.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrderEvent>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(configs, new org.apache.kafka.common.serialization.StringDeserializer(),
                new JsonDeserializer<>(OrderEvent.class, false)));
        return factory;
    }
}
