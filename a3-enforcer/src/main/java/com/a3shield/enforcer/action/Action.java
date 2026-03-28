package com.a3shield.enforcer.action;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Action {
    private String name; // read, write, delete
}