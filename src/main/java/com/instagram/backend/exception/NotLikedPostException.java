package com.instagram.backend.exception;

public class NotLikedPostException extends RuntimeException{
    public NotLikedPostException(String message) {
        super(message);
    }
}
