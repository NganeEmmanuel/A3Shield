package com.a3shield.spring.security;

import com.a3shield.enforcer.model.Subject;

/**
 * Extracts the {@link Subject} for the current request from the host
 * application's identity solution (Spring Security context / JWT by default —
 * see {@link SpringSecuritySubjectContextExtractor}). Host apps provide their
 * own implementation to plug in a different identity source (ADR-0001).
 */
public interface SubjectExtractor {
    Subject extractSubject();
}
