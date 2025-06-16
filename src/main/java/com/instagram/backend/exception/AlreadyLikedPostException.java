package com.instagram.backend.exception;

public class AlreadyLikedPostException extends RuntimeException{
    public AlreadyLikedPostException(String message) {
        super(message);
    }
}
