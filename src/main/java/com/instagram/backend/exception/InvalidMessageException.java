package com.instagram.backend.exception;

public class InvalidMessageException extends RuntimeException{
    public InvalidMessageException(String message) {
        super(message);
    }
}
