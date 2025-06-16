package com.instagram.backend.exception;

public class AccountAlreadyVerifiedException extends RuntimeException{
    public AccountAlreadyVerifiedException(String message) {
        super(message);
    }
}
