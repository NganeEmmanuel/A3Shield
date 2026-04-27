package com.a3shield.spring.security;

import com.a3shield.enforcer.model.*;
import org.springframework.stereotype.Component;

/**
 * Extracts Subject from Spring Security context (JWT).
 */
@Component
public interface SubjectExtractor {
    Subject extractSubject();
}