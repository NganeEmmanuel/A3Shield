package com.a3shield.enforcer.permission;

import lombok.Builder;

import java.time.LocalDateTime;


@Builder
public record Permission (
    String id,
    String name,
    String description,
    LocalDateTime createdAt,
    LocalDateTime modified
){}
