package com.instagram.backend.exception;

public class NotFollowingException extends RuntimeException{
    public NotFollowingException(String message) {
        super(message);
    }
}
