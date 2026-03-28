package com.a3shield.enforcer.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Action {
    private String name; // read, write, delete
}