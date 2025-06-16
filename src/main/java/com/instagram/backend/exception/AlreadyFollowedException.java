package com.instagram.backend.exception;

public class AlreadyFollowedException extends RuntimeException{
    public AlreadyFollowedException(String message) {
        super(message);
    }
}
