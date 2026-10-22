package com.a3shield.spring.context;

import com.a3shield.core.model.Subject;
import com.a3shield.spring.security.SubjectExtractor;

public class SubjectContext {
    private static final ThreadLocal<Subject> CURRENT_SUBJECT = new ThreadLocal<>();
    private static SubjectExtractor subjectExtractor;

    public SubjectContext(SubjectExtractor subjectExtractor) {
        SubjectContext.subjectExtractor = subjectExtractor;
    }

    public static Subject getCurrentSubject() {

        if (CURRENT_SUBJECT.get() == null) {
            setCurrentSubject(subjectExtractor.extractSubject());
        }
        ;
        return CURRENT_SUBJECT.get();
    }

    public static void setCurrentSubject(Subject subject) {
        CURRENT_SUBJECT.set(subject);
    }

    public static void clearCurrentSubject() {
        CURRENT_SUBJECT.remove();
    }
}
