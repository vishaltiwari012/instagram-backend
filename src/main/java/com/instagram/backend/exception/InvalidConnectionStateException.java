package com.instagram.backend.exception;

public class InvalidConnectionStateException extends RuntimeException{
    public InvalidConnectionStateException(String message) {
        super(message);
    }
}
