package com.instagram.backend.exception;

public class InvalidSenderException extends RuntimeException{
    public InvalidSenderException(String message) {
        super(message);
    }
}
