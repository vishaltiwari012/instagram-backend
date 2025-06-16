package com.instagram.backend.exception;

public class AlreadyBlockedException extends RuntimeException{
    public AlreadyBlockedException(String message) {
        super(message);
    }
}
