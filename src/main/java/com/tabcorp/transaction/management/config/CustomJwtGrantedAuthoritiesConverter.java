package com.tabcorp.transaction.management.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles"); // Extract roles claim from JWT
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role)) // Map roles to Spring Security authorities
            .collect(Collectors.toList());
    }
}
