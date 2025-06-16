package com.instagram.backend.exception;

public class CommentAlreadyLikedException extends RuntimeException{
    public CommentAlreadyLikedException(String message) {
        super(message);
    }
}
