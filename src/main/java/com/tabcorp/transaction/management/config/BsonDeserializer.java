package com.tabcorp.transaction.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tabcorp.transaction.management.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
public class BsonDeserializer implements Deserializer<Transaction> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Nothing to configure
    }

    @Override
    public Transaction deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        
        try {
            Transaction transaction = objectMapper.readValue(data, Transaction.class);
            transaction.setDataFormat("BSON");
            return transaction;
        } catch (IOException e) {
            log.error("Error deserializing BSON data: {}", e.getMessage(), e);
            throw new SerializationException("Error deserializing BSON data", e);
        }
    }

    @Override
    public void close() {
        // Nothing to close
    }
}

