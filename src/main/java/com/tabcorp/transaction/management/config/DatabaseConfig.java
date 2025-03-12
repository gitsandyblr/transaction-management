package com.tabcorp.transaction.management.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.h2.H2ConnectionOption;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.cache.annotation.EnableCaching;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.tabcorp.transaction.management.repository")
@EnableTransactionManagement
@EnableCaching
public class DatabaseConfig extends AbstractR2dbcConfiguration {

    @Value("${spring.r2dbc.url}")
    private String r2dbcUrl;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    @Value("${spring.r2dbc.pool.initial-size:10}")
    private int initialPoolSize;

    @Value("${spring.r2dbc.pool.max-size:30}")
    private int maxPoolSize;

    @Value("${spring.r2dbc.pool.max-idle-time:30m}")
    private Duration maxIdleTime;

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.timeout:2000}")
    private int redisTimeout;

    @Bean
    @Override
    public ConnectionFactory connectionFactory() {
        // Parse URL to get database name
        // Expected format: r2dbc:h2:mem:///transaction-data-repo
        String databaseName = "transaction-data-repo";
        if (r2dbcUrl.contains("///")) {
            String[] parts = r2dbcUrl.split("///");
            if (parts.length > 1) {
                databaseName = parts[1];
            }
        }

        H2ConnectionConfiguration h2Config = H2ConnectionConfiguration.builder()
                .inMemory(databaseName)
                .property(H2ConnectionOption.DB_CLOSE_DELAY, "-1") // Keeps the DB alive
                .username(username)
                .password(password)
                .build();

        // Creating a connection pool with the H2 connection factory
        ConnectionFactory connectionFactory = new H2ConnectionFactory(h2Config);
        ConnectionPoolConfiguration poolConfiguration = ConnectionPoolConfiguration.builder()
                .connectionFactory(connectionFactory)
                .initialSize(initialPoolSize)
                .maxSize(maxPoolSize)
                .maxIdleTime(maxIdleTime)
                .validationQuery("SELECT 1")
                .build();

        return new ConnectionPool(poolConfiguration);
    }

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisHost);
        redisConfig.setPort(redisPort);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisTimeout))
                .shutdownTimeout(Duration.ZERO)
                .build();

        return new LettuceConnectionFactory(redisConfig, clientConfig);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(@Qualifier("reactiveRedisConnectionFactory") ReactiveRedisConnectionFactory factory, ObjectMapper objectMapper) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, Object> context = builder
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    /**
     * Configure the R2DBC mapping context with our naming strategy.
     * This handles the automatic conversion for entity fields.
     */
    @Bean
    public R2dbcMappingContext r2dbcMappingContext(NamingStrategy namingStrategy) {
        R2dbcMappingContext mappingContext = new R2dbcMappingContext(namingStrategy);
        mappingContext.setForceQuote(false);
        return mappingContext;
    }

    @Bean
    @Override
    public R2dbcCustomConversions r2dbcCustomConversions() {
        List<Converter<?, ?>> converters = Arrays.asList(
                new JsonToByteBufferConverter(),
                new ByteBufferToJsonConverter()
        );
        return new R2dbcCustomConversions(getStoreConversions(), converters);
    }

    // Custom converters for JSON and BSON handling
    @WritingConverter
    public static class JsonToByteBufferConverter implements Converter<String, ByteBuffer> {
        @Override
        public ByteBuffer convert(String json) {
            return ByteBuffer.wrap(json.getBytes());
        }
    }

    @ReadingConverter
    public static class ByteBufferToJsonConverter implements Converter<ByteBuffer, String> {
        @Override
        public String convert(ByteBuffer byteBuffer) {
            // Make a copy of the byteBuffer to avoid affecting the original position
            ByteBuffer copy = byteBuffer.duplicate();
            byte[] bytes = new byte[copy.remaining()];
            copy.get(bytes);
            
            // Try to detect if this is valid UTF-8 text
            try {
                CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
                decoder.decode(ByteBuffer.wrap(bytes));
                // If we get here, it's valid UTF-8, so return it as a string
                return new String(bytes, StandardCharsets.UTF_8);
            } catch (CharacterCodingException e) {
                // Not valid UTF-8 (likely binary BSON data)
                // Return Base64 encoded version with a prefix to indicate this is binary data
                return "BINARY:" + Base64.getEncoder().encodeToString(bytes);
            }
        }
    }
}