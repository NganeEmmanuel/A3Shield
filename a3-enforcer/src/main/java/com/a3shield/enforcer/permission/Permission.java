package com.a3shield.enforcer.permission;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record Permission(String id, String name, String description, LocalDateTime createdAt, LocalDateTime modified) {}
