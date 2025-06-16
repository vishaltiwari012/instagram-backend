package com.instagram.backend.exception;

public class CommentNotLikedException extends RuntimeException{
    public CommentNotLikedException(String message) {
        super(message);
    }
}
