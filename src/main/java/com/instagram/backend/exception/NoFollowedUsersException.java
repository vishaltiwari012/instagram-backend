package com.instagram.backend.exception;

public class NoFollowedUsersException extends RuntimeException{
    public NoFollowedUsersException(String message) {
        super(message);
    }
}
