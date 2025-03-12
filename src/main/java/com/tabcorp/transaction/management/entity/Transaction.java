package com.tabcorp.transaction.management.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.bson.BsonBinaryWriter;
import org.bson.Document;
import org.bson.codecs.BsonValueCodecProvider;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.io.BasicOutputBuffer;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Map;

@Table("customer_transaction")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    private Long id;

    @Column("transaction_time")
    private LocalDateTime transactionTime;

    @Column("customer_id")
    private Integer customerId;

    @Column("product_code")
    private String productCode;

    @Column("quantity")
    private Integer quantity;

    @Column("data_format")
    private String dataFormat;

    @Column("json_data")
    private String jsonData;

    @Column("bson_data")
    private byte[] bsonData;

    @Column("processed_time")
    private LocalDateTime processedTime;

    @Column("status")
    private String status;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final CodecRegistry codecRegistry = CodecRegistries.fromProviders(
            new ValueCodecProvider(),
            new BsonValueCodecProvider()
    );
    @Transient
    public Map<String, Object> getDataAsMap() {
        String data = null;
        
        // Determine which data field to use based on format
        if (isJsonFormat() && jsonData != null && !jsonData.isEmpty()) {
            data = jsonData;
            
            try {
                return objectMapper.readValue(data, Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse JSON data: " + e.getMessage(), e);
            }
        } else if (isBsonFormat() && bsonData != null && bsonData.length > 0) {
            try {
                // First, check if we're dealing with a BINARY: prefix from the custom converter
                String bsonString = new String(bsonData, StandardCharsets.UTF_8);
                if (bsonString.startsWith("BINARY:")) {
                    // This is Base64 encoded binary data, decode it first
                    String base64Data = bsonString.substring("BINARY:".length());
                    bsonData = Base64.getDecoder().decode(base64Data);
                }
                
                // Try to parse as a JSON string first (for backward compatibility)
                try {
                    Document document = Document.parse(new String(bsonData, StandardCharsets.UTF_8));
                    if (document != null) {
                        return document;
                    }
                } catch (Exception jsonParseException) {
                    // Not valid JSON, continue to binary BSON handling
                }
                
                // Handle as true binary BSON data
                org.bson.BsonBinaryReader reader = new org.bson.BsonBinaryReader(
                        java.nio.ByteBuffer.wrap(bsonData));
                org.bson.codecs.DecoderContext decoderContext = org.bson.codecs.DecoderContext.builder().build();
                DocumentCodec documentCodec = new DocumentCodec(codecRegistry);
                Document document = documentCodec.decode(reader, decoderContext);
                reader.close();
                return document;
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse BSON data: " + e.getMessage(), e);
            }
        }
        
        return null;
    }
    
    @Transient
    public void setDataAsMap(Map<String, Object> dataMap) {
        if (dataMap == null) {
            this.jsonData = null;
            this.bsonData = null;
            return;
        }
        
        try {
            // Store in appropriate field based on format
            if (isJsonFormat()) {
                this.jsonData = objectMapper.writeValueAsString(dataMap);
                this.bsonData = null;
            } else if (isBsonFormat()) {
                // Create a Document from the Map
                Document document = new Document(dataMap);
                
                // Convert Document to proper BSON binary format
                BasicOutputBuffer buffer = new BasicOutputBuffer();
                BsonBinaryWriter writer = new BsonBinaryWriter(buffer);
                
                // Use the DocumentCodec to write the document to the writer
                DocumentCodec codec = new DocumentCodec(codecRegistry);
                codec.encode(writer, document, EncoderContext.builder().build());
                
                this.bsonData = buffer.toByteArray();
                writer.close();
                this.jsonData = null;
            } else {
                // Default to JSON if no format specified
                this.dataFormat = "JSON";
                this.jsonData = objectMapper.writeValueAsString(dataMap);
                this.bsonData = null;
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize data map: " + e.getMessage(), e);
        }
    }


    @Transient
    public boolean isJsonFormat(){
            return "JSON".equalsIgnoreCase(dataFormat);
    }

    @Transient
    public boolean isBsonFormat() {
        return "BSON".equalsIgnoreCase(dataFormat);
    }

}
