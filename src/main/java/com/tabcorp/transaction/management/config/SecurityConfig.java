package com.tabcorp.transaction.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // Use a 256-bit (32 character) key for HMAC-SHA256
    private static final String SECRET_KEY = "transaction_management_secret_key_32bytes"; // Same as used in MockAuthController

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless session management
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Authorize requests based on roles/scopes in the JWT
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/public/**", "/oauth/token").permitAll() // Public endpoints and OAuth token endpoint
                .requestMatchers("/api/**").hasAuthority("SCOPE_api.read") // Protected endpoints require "api.read" scope
                .anyRequest().authenticated())
            // Configure channel security - HTTP vs HTTPS
            .requiresChannel(channel -> channel
                .anyRequest().requiresSecure()
            ) // Require HTTPS for all requests
            // Disable CSRF (not needed for stateless APIs)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/oauth/token")) // Disable CSRF for H2 console and OAuth token endpoint
            // Configure headers to allow frames for H2 console
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("frame-ancestors 'self'")))
            // Configure OAuth2 Resource Server with JWT support
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] secretKeyBytes = SECRET_KEY.getBytes();
        return NimbusJwtDecoder.withSecretKey(new javax.crypto.spec.SecretKeySpec(secretKeyBytes, "HmacSHA256")).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_"); // Map scopes to Spring Security authorities

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }
}
