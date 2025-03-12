package com.tabcorp.transaction.management.config;

import com.tabcorp.transaction.management.entity.Transaction;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class KafkaConsumerConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.batch-size:100}")
    private int batchSize;

    @Value("${kafka.consumer.concurrency:3}")
    private int concurrency;

    @Bean
    public ConsumerFactory<String, Transaction> jsonConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024 * 1024);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        JsonDeserializer<Transaction> deserializer = new JsonDeserializer<>(Transaction.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("com.tabcorp.transaction.management.entity");
        
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConsumerFactory<String, Transaction> bsonConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "com.tabcorp.transaction.management.config.BsonDeserializer");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, batchSize);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024 * 1024);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Transaction> jsonKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Transaction> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(jsonConsumerFactory());
        factory.setBatchListener(true);
        factory.setConcurrency(concurrency);
        factory.setBatchMessageConverter(new BatchMessagingMessageConverter());
        factory.setCommonErrorHandler(kafkaErrorHandler());
        factory.getContainerProperties().setIdleBetweenPolls(0);
        factory.getContainerProperties().setPollTimeout(1000);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Transaction> bsonKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Transaction> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(bsonConsumerFactory());
        factory.setBatchListener(true);
        factory.setConcurrency(concurrency);
        factory.setBatchMessageConverter(new BatchMessagingMessageConverter());
        factory.setCommonErrorHandler(kafkaErrorHandler());
        factory.getContainerProperties().setIdleBetweenPolls(0);
        factory.getContainerProperties().setPollTimeout(1000);
        return factory;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            (record, exception) -> {
                log.error("Error in processing: {}", exception.getMessage(), exception);
                // Add metrics and alerting here
            },
            new FixedBackOff(1000L, 3L)  // Retry 3 times with 1 second delay
        );
        return errorHandler;
    }
}

