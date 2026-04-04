package com.a3shield.spring.security;

import com.a3shield.enforcer.model.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts Subject from Spring Security context (JWT).
 */
public class SecurityContextExtractor {

    public Subject extractSubject() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            return buildSubject(jwt);
        }

        return null;
    }

    private Subject buildSubject(Jwt jwt) {

        String id = jwt.getSubject();

        String type = jwt.getClaimAsString("type");
        List<String> roles = jwt.getClaimAsStringList("roles");

        // add all other claims as attributes (optional - depends on your needs)

        Map<String, Object> attributes = new HashMap<>(jwt.getClaims());

        return Subject.builder()
                .id(id)
                .type(type != null ? SubjectType.valueOf(type) : SubjectType.USER)
                .roles(roles != null ? Set.copyOf(roles) : Set.of())
                .attributes(attributes)
                .build();
    }
}