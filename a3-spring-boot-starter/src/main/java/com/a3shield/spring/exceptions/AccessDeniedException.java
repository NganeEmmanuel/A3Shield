package com.a3shield.spring.exceptions;

public class AccessDeniedException extends Exception{
    public AccessDeniedException(String message){
        super(message);
    }
}
